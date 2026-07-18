package com.transport.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.transport.transport.model.MajorationTarif;
import com.transport.transport.model.TarificationVehicule;
import com.transport.transport.model.TypeVehicule;
import com.transport.transport.repository.MajorationTarifRepository;
import com.transport.transport.repository.TarificationVehiculeRepository;

@Service
public class TarificationService {
    private static final int MONEY_SCALE = 3;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final TarificationVehiculeRepository tarifRepository;
    private final MajorationTarifRepository majorationRepository;

    public TarificationService(
            TarificationVehiculeRepository tarifRepository,
            MajorationTarifRepository majorationRepository) {
        this.tarifRepository = tarifRepository;
        this.majorationRepository = majorationRepository;
    }

    public BigDecimal calculate(TypeVehicule vehicule, double distanceKm) {
        return calculateDetailed(vehicule, distanceKm, LocalDateTime.now()).prix();
    }

    public TarificationResult calculateDetailed(
            TypeVehicule vehicule,
            double distanceKm,
            LocalDateTime dateReference) {
        if (vehicule == null) {
            throw new IllegalArgumentException("Vehicule obligatoire pour calculer le tarif");
        }
        if (!Double.isFinite(distanceKm) || distanceKm <= 0) {
            throw new IllegalArgumentException("Distance routiere invalide");
        }

        LocalDateTime reference = dateReference != null ? dateReference : LocalDateTime.now();
        TarifComponents components = tarifRepository
                .findFirstByTypeVehiculeAndDateFinIsNullAndDateDebutLessThanEqualOrderByDateDebutDesc(vehicule, reference)
                .map(this::fromDatabase)
                .orElseGet(() -> fallback(vehicule));

        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        BigDecimal prix = components.start().add(distance.multiply(components.perKm()));
        BigDecimal prixLivreur = components.startCourier().add(distance.multiply(components.perKmCourier()));

        Optional<MajorationTarif> activeMajoration = majorationRepository.findFirstActiveAt(reference);
        BigDecimal pourcentage = activeMajoration.map(MajorationTarif::getPourcentageAjout).orElse(BigDecimal.ZERO);
        BigDecimal multiplier = BigDecimal.ONE.add(pourcentage.divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP));
        prix = scale(prix.multiply(multiplier));
        prixLivreur = scale(prixLivreur.multiply(multiplier));
        BigDecimal prixSociete = scale(prix.subtract(prixLivreur));

        if (prixSociete.signum() < 0 || prixLivreur.add(prixSociete).compareTo(prix) != 0) {
            throw new IllegalStateException("Repartition tarifaire invalide");
        }

        return new TarificationResult(
                prix,
                prixLivreur,
                prixSociete,
                scale(components.start()),
                scale(components.startCourier()),
                scale(components.startCompany()),
                scale(components.perKm()),
                scale(components.perKmCourier()),
                scale(components.perKmCompany()),
                components.tarifId(),
                activeMajoration.map(MajorationTarif::getId).orElse(null),
                pourcentage,
                components.fallback());
    }

    private TarifComponents fromDatabase(TarificationVehicule tarif) {
        return new TarifComponents(
                tarif.getPrixCommencement(),
                tarif.getPrixCommencementLivreur(),
                tarif.getPrixCommencementSociete(),
                tarif.getPrixParKilometre(),
                tarif.getPrixParKilometreLivreur(),
                tarif.getPrixParKilometreSociete(),
                tarif.getId(),
                false);
    }

    private TarifComponents fallback(TypeVehicule vehicule) {
        BigDecimal start;
        BigDecimal perKm;
        switch (vehicule) {
            case VEHICULE_PARTICULIER -> { start = new BigDecimal("3.5"); perKm = new BigDecimal("0.5"); }
            case DEUX_ROUES_MOTORISES -> { start = new BigDecimal("5"); perKm = new BigDecimal("0.3"); }
            case FOURGON_MINIBUS -> { start = new BigDecimal("3"); perKm = new BigDecimal("0.7"); }
            case GROS_UTILITAIRE -> { start = new BigDecimal("10"); perKm = BigDecimal.ONE; }
            case VEHICULE_UTILITAIRE_LEGER -> { start = new BigDecimal("7"); perKm = new BigDecimal("0.8"); }
            default -> throw new IllegalArgumentException("Vehicule non supporte");
        }
        BigDecimal startCourier = start.divide(new BigDecimal("2"), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal perKmCourier = perKm.divide(new BigDecimal("2"), MONEY_SCALE, RoundingMode.HALF_UP);
        return new TarifComponents(
                start,
                startCourier,
                start.subtract(startCourier),
                perKm,
                perKmCourier,
                perKm.subtract(perKmCourier),
                null,
                true);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record TarifComponents(
            BigDecimal start,
            BigDecimal startCourier,
            BigDecimal startCompany,
            BigDecimal perKm,
            BigDecimal perKmCourier,
            BigDecimal perKmCompany,
            String tarifId,
            boolean fallback) {}

    public record TarificationResult(
            BigDecimal prix,
            BigDecimal prixLivreur,
            BigDecimal prixSociete,
            BigDecimal prixCommencement,
            BigDecimal prixCommencementLivreur,
            BigDecimal prixCommencementSociete,
            BigDecimal prixParKilometre,
            BigDecimal prixParKilometreLivreur,
            BigDecimal prixParKilometreSociete,
            String tarificationVehiculeId,
            String majorationTarifId,
            BigDecimal pourcentageMajoration,
            boolean tarifFallback) {}
}
