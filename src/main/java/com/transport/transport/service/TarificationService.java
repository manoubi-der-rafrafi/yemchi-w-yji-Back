package com.transport.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.transport.transport.model.TypeVehicule;

@Service
public class TarificationService {

    public BigDecimal calculate(TypeVehicule vehicule, double distanceKm) {
        if (vehicule == null) {
            throw new IllegalArgumentException("Vehicule obligatoire pour calculer le tarif");
        }
        if (!Double.isFinite(distanceKm) || distanceKm <= 0) {
            throw new IllegalArgumentException("Distance routiere invalide");
        }

        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        BigDecimal total = switch (vehicule) {
            case VEHICULE_PARTICULIER -> new BigDecimal("3.5").add(distance.multiply(new BigDecimal("0.5")));
            case DEUX_ROUES_MOTORISES -> new BigDecimal("5").add(distance.multiply(new BigDecimal("0.3")));
            case FOURGON_MINIBUS -> new BigDecimal("3").add(distance.multiply(new BigDecimal("0.7")));
            case GROS_UTILITAIRE -> new BigDecimal("10").add(distance);
            case VEHICULE_UTILITAIRE_LEGER -> new BigDecimal("7").add(distance.multiply(new BigDecimal("0.8")));
        };
        return total.setScale(3, RoundingMode.HALF_UP);
    }
}
