package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.transport.transport.model.TarificationVehicule;
import com.transport.transport.model.TypeVehicule;
import com.transport.transport.repository.MajorationTarifRepository;
import com.transport.transport.repository.TarificationVehiculeRepository;

class TarificationServiceTest {
    private TarificationVehiculeRepository tarifRepository;
    private MajorationTarifRepository majorationRepository;
    private TarificationService service;

    @BeforeEach
    void setUp() {
        tarifRepository = org.mockito.Mockito.mock(TarificationVehiculeRepository.class);
        majorationRepository = org.mockito.Mockito.mock(MajorationTarifRepository.class);
        when(majorationRepository.findFirstActiveAt(any())).thenReturn(Optional.empty());
        service = new TarificationService(tarifRepository, majorationRepository);
    }

    @Test
    void usesLegacyFallbackWhenNoDatabaseTariffExists() {
        when(tarifRepository.findFirstByTypeVehiculeAndDateFinIsNullAndDateDebutLessThanEqualOrderByDateDebutDesc(any(), any()))
                .thenReturn(Optional.empty());

        var result = service.calculateDetailed(
                TypeVehicule.DEUX_ROUES_MOTORISES,
                10,
                LocalDateTime.of(2026, 7, 18, 10, 0));

        assertEquals(new BigDecimal("8.000"), result.prix());
        assertEquals(new BigDecimal("4.000"), result.prixLivreur());
        assertEquals(new BigDecimal("4.000"), result.prixSociete());
        assertTrue(result.tarifFallback());
    }

    @Test
    void usesActiveDatabaseTariffAndPreservesPriceSplit() {
        TarificationVehicule tarif = new TarificationVehicule();
        tarif.setId("tarif-1");
        tarif.setPrixCommencement(new BigDecimal("5"));
        tarif.setPrixCommencementLivreur(new BigDecimal("3"));
        tarif.setPrixCommencementSociete(new BigDecimal("2"));
        tarif.setPrixParKilometre(new BigDecimal("2"));
        tarif.setPrixParKilometreLivreur(new BigDecimal("1.2"));
        tarif.setPrixParKilometreSociete(new BigDecimal("0.8"));
        when(tarifRepository.findFirstByTypeVehiculeAndDateFinIsNullAndDateDebutLessThanEqualOrderByDateDebutDesc(any(), any()))
                .thenReturn(Optional.of(tarif));

        var result = service.calculateDetailed(
                TypeVehicule.DEUX_ROUES_MOTORISES,
                10,
                LocalDateTime.of(2026, 7, 18, 10, 0));

        assertEquals(new BigDecimal("25.000"), result.prix());
        assertEquals(new BigDecimal("15.000"), result.prixLivreur());
        assertEquals(new BigDecimal("10.000"), result.prixSociete());
        assertEquals(result.prix(), result.prixLivreur().add(result.prixSociete()));
    }
}
