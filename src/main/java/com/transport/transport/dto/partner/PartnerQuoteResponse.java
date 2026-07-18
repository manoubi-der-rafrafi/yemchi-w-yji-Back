package com.transport.transport.dto.partner;

import java.math.BigDecimal;

import com.transport.transport.model.TypeVehicule;

public record PartnerQuoteResponse(
        TypeVehicule vehicule,
        double distanceKm,
        long dureeMinutes,
        BigDecimal fraisLivraison) {}
