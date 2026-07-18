package com.transport.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.transport.transport.model.TarificationVehicule;
import com.transport.transport.repository.TarificationVehiculeRepository;

@Service
public class TarificationVehiculeAdminService {
    private static final int MONEY_SCALE = 3;
    private final TarificationVehiculeRepository repository;

    public TarificationVehiculeAdminService(TarificationVehiculeRepository repository) {
        this.repository = repository;
    }

    public List<TarificationVehicule> list() {
        return repository.findAllByOrderByDateDebutDesc();
    }

    public TarificationVehicule activate(TarificationVehicule tarif, String adminId) {
        validate(tarif);
        LocalDateTime startsAt = tarif.getDateDebut();
        List<TarificationVehicule> current = repository.findByTypeVehiculeAndDateFinIsNull(tarif.getTypeVehicule());
        for (TarificationVehicule existing : current) {
            if (existing.getDateDebut() != null && startsAt.isBefore(existing.getDateDebut())) {
                throw new IllegalArgumentException("La date de debut doit etre posterieure au tarif actuel");
            }
            existing.setDateFin(startsAt);
        }
        if (!current.isEmpty()) {
            repository.saveAll(current);
        }

        tarif.setId(null);
        tarif.setDateFin(null);
        tarif.setCreatedByAdminId(adminId);
        normalize(tarif);
        return repository.save(tarif);
    }

    private void validate(TarificationVehicule tarif) {
        if (tarif == null || tarif.getTypeVehicule() == null || tarif.getDateDebut() == null) {
            throw new IllegalArgumentException("Type de vehicule et date de debut obligatoires");
        }
        if (tarif.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Un tarif courant ne peut pas commencer dans le futur");
        }
        requireNonNegative(tarif.getPrixCommencement(), "prixCommencement");
        requireNonNegative(tarif.getPrixCommencementLivreur(), "prixCommencementLivreur");
        requireNonNegative(tarif.getPrixParKilometre(), "prixParKilometre");
        requireNonNegative(tarif.getPrixParKilometreLivreur(), "prixParKilometreLivreur");
        if (tarif.getPrixCommencementLivreur().compareTo(tarif.getPrixCommencement()) > 0
                || tarif.getPrixParKilometreLivreur().compareTo(tarif.getPrixParKilometre()) > 0) {
            throw new IllegalArgumentException("La part livreur ne peut pas depasser le prix total");
        }

        BigDecimal expectedStartCompany = tarif.getPrixCommencement().subtract(tarif.getPrixCommencementLivreur());
        BigDecimal expectedKmCompany = tarif.getPrixParKilometre().subtract(tarif.getPrixParKilometreLivreur());
        if (tarif.getPrixCommencementSociete() != null
                && tarif.getPrixCommencementSociete().compareTo(expectedStartCompany) != 0) {
            throw new IllegalArgumentException("Les parts de commencement ne correspondent pas au prix total");
        }
        if (tarif.getPrixParKilometreSociete() != null
                && tarif.getPrixParKilometreSociete().compareTo(expectedKmCompany) != 0) {
            throw new IllegalArgumentException("Les parts kilometriques ne correspondent pas au prix total");
        }
        tarif.setPrixCommencementSociete(expectedStartCompany);
        tarif.setPrixParKilometreSociete(expectedKmCompany);
    }

    private void normalize(TarificationVehicule tarif) {
        tarif.setPrixCommencement(scale(tarif.getPrixCommencement()));
        tarif.setPrixCommencementLivreur(scale(tarif.getPrixCommencementLivreur()));
        tarif.setPrixCommencementSociete(scale(tarif.getPrixCommencementSociete()));
        tarif.setPrixParKilometre(scale(tarif.getPrixParKilometre()));
        tarif.setPrixParKilometreLivreur(scale(tarif.getPrixParKilometreLivreur()));
        tarif.setPrixParKilometreSociete(scale(tarif.getPrixParKilometreSociete()));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private void requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " doit etre positif ou nul");
        }
    }
}
