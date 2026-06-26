package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.transport.dto.partner.PartnerCreateCommandeRequest;
import com.transport.transport.model.TypeVehicule;

class VehicleAnalysisServiceTest {

    @Test
    void resolveVehicleForPartnerProductsFallsBackLocallyWhenWebhookUrlMissing() {
        MailService mailService = org.mockito.Mockito.mock(MailService.class);
        VehicleAnalysisService service = new VehicleAnalysisService(
                mailService,
                new ObjectMapper(),
                "",
                "alerts@example.com",
                8000);

        TypeVehicule selected = service.resolveVehicleForPartnerProducts(List.of(
                new PartnerCreateCommandeRequest.ProductItem(
                        "Confiture de Cerise Noire",
                        "Confitures & Compotes",
                        1,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        null,
                        null,
                        null,
                        null)));

        assertEquals(TypeVehicule.DEUX_ROUES_MOTORISES, selected);
        verify(mailService, never()).sendTextEmail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
