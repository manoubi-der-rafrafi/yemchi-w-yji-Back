package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.transport.transport.dto.partner.PartnerCreateCommandeRequest;
import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;
import com.transport.transport.model.TypeVehicule;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.security.PartnerPrincipal;

class PartnerCommandeServiceTest {

    private CommandeRepository commandeRepository;
    private ProduitService produitService;
    private UtilisateurRepository utilisateurRepository;
    private VehicleAnalysisService vehicleAnalysisService;
    private PartnerCommandeService service;

    @BeforeEach
    void setUp() {
        commandeRepository = org.mockito.Mockito.mock(CommandeRepository.class);
        produitService = org.mockito.Mockito.mock(ProduitService.class);
        utilisateurRepository = org.mockito.Mockito.mock(UtilisateurRepository.class);
        vehicleAnalysisService = org.mockito.Mockito.mock(VehicleAnalysisService.class);
        service = new PartnerCommandeService(
                commandeRepository,
                produitService,
                utilisateurRepository,
                vehicleAnalysisService);
    }

    @Test
    void createConfirmedCommandeStoresExternalIdsAndProducts() {
        PartnerPrincipal principal = new PartnerPrincipal(
                "partner-1",
                "12",
                "Boutique Test",
                java.util.Set.of("delivery:create"),
                "pk_live_test");

        PartnerCreateCommandeRequest request = new PartnerCreateCommandeRequest(
                "ORDER-1001",
                "fragile",
                BigDecimal.valueOf(55),
                Commande.ModePaiement.EN_LIGNE,
                new PartnerCreateCommandeRequest.ContactPoint("Boutique", "111", "Depart", 36.8, 10.1),
                new PartnerCreateCommandeRequest.ContactPoint("Client", "222", "Arrivee", 36.9, 10.2),
                List.of(new PartnerCreateCommandeRequest.ProductItem(
                        "Chaise",
                        "meuble",
                        1,
                        BigDecimal.valueOf(12),
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(80),
                        "bois",
                        "img1",
                        null,
                        null)),
                null);

        when(commandeRepository.findByPartenaireIdAndExternalOrderId("partner-1", "ORDER-1001"))
                .thenReturn(Optional.empty());
        when(commandeRepository.save(any(Commande.class))).thenAnswer(invocation -> {
            Commande commande = invocation.getArgument(0);
            commande.setId("cmd-1");
            return commande;
        });
        when(produitService.createProduits(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(vehicleAnalysisService.resolveVehicleForPartnerProducts(any()))
                .thenReturn(TypeVehicule.VEHICULE_PARTICULIER);

        var response = service.createConfirmedCommande(principal, request);

        assertEquals("ORDER-1001", response.commande().getExternalOrderId());
        assertEquals("12", response.commande().getExternalBusinessId());
        assertEquals("partner-1", response.commande().getPartenaireId());
        assertEquals(Commande.Statut.confirmer, response.commande().getStatut());
        assertNotNull(response.commande().getVehicule());
        Produit produit = response.produits().get(0);
        assertEquals("cmd-1", produit.getCommandeId());
        assertEquals("Chaise", produit.getNom());
    }
}
