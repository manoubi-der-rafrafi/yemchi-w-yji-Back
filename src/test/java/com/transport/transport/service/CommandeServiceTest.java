package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;
import com.transport.transport.model.TypeVehicule;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.ProduitRepository;
import com.transport.transport.repository.UtilisateurRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CommandeServiceTest {

    private CommandeRepository commandeRepository;
    private ProduitRepository produitRepository;
    private UtilisateurRepository utilisateurRepository;
    private VehicleAnalysisService vehicleAnalysisService;
    private CommandeGeographyService commandeGeographyService;
    private CommandeService service;

    @BeforeEach
    void setUp() {
        commandeRepository = org.mockito.Mockito.mock(CommandeRepository.class);
        produitRepository = org.mockito.Mockito.mock(ProduitRepository.class);
        utilisateurRepository = org.mockito.Mockito.mock(UtilisateurRepository.class);
        vehicleAnalysisService = org.mockito.Mockito.mock(VehicleAnalysisService.class);
        commandeGeographyService = new CommandeGeographyService();
        service = new CommandeService(
                commandeRepository,
                produitRepository,
                utilisateurRepository,
                vehicleAnalysisService,
                commandeGeographyService);
    }

    @Test
    void getCommandeEnCoursByClientIdRebuildsSingleCommandeWhenDuplicatesExist() {
        String clientId = "client-1";

        Commande older = new Commande();
        older.setId("cmd-old");
        older.setClientId(clientId);
        older.setStatut(Commande.Statut.en_cours);
        older.setDestination("Ariana");
        older.setNomArrivee("Client A");
        older.setDateDemande(LocalDateTime.of(2026, 6, 26, 10, 0));

        Commande newer = new Commande();
        newer.setId("cmd-new");
        newer.setClientId(clientId);
        newer.setStatut(Commande.Statut.en_cours);
        newer.setLocalisationDepart("Tunis");
        newer.setNomDepart("Boutique");
        newer.setPrix(BigDecimal.valueOf(15));
        newer.setDateDemande(LocalDateTime.of(2026, 6, 26, 11, 0));

        Produit produitA = new Produit();
        produitA.setId("prod-1");
        produitA.setCommandeId("cmd-old");
        Produit produitB = new Produit();
        produitB.setId("prod-2");
        produitB.setCommandeId("cmd-new");

        when(commandeRepository.findByClientIdAndStatutOrderByDateDemandeDesc(
                clientId,
                Commande.Statut.en_cours))
                .thenReturn(List.of(newer, older));
        when(produitRepository.findByCommandeIdIn(List.of("cmd-new", "cmd-old")))
                .thenReturn(List.of(produitA, produitB));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(invocation -> {
            Commande saved = invocation.getArgument(0);
            saved.setId("cmd-rebuilt");
            return saved;
        });
        when(produitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Commande result = service.getCommandeEnCoursByClientId(clientId).orElseThrow();

        assertEquals("cmd-rebuilt", result.getId());
        assertEquals(clientId, result.getClientId());
        assertEquals(Commande.Statut.en_cours, result.getStatut());
        assertEquals("Tunis", result.getLocalisationDepart());
        assertEquals("Ariana", result.getDestination());
        assertEquals("Boutique", result.getNomDepart());
        assertEquals("Client A", result.getNomArrivee());
        assertEquals(BigDecimal.valueOf(15), result.getPrix());
        assertNotNull(result.getMajLe());
        assertEquals("cmd-rebuilt", produitA.getCommandeId());
        assertEquals("cmd-rebuilt", produitB.getCommandeId());

        verify(produitRepository).saveAll(List.of(produitA, produitB));
        verify(commandeRepository).deleteAll(List.of(newer, older));
    }

    @Test
    void prepareCommandeVehicleUsesFallbackDistanceWhenRoutingServiceFails() {
        RoutingService routingService = org.mockito.Mockito.mock(RoutingService.class);
        TarificationService tarificationService = org.mockito.Mockito.mock(TarificationService.class);
        CommandeService quoteService = new CommandeService(
                commandeRepository,
                produitRepository,
                utilisateurRepository,
                vehicleAnalysisService,
                commandeGeographyService,
                null,
                routingService,
                tarificationService);

        Commande commande = new Commande();
        commande.setId("cmd-route-fallback");
        commande.setLatitudeDepart(36.8689);
        commande.setLongitudeDepart(10.3417);
        commande.setLatitudeDestination(36.8840);
        commande.setLongitudeDestination(10.1658);

        when(commandeRepository.findById("cmd-route-fallback")).thenReturn(java.util.Optional.of(commande));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(routingService.calculateRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service de routage indisponible"));
        when(vehicleAnalysisService.resolveVehicleForProduits(any(), anyDouble()))
                .thenReturn(TypeVehicule.DEUX_ROUES_MOTORISES);
        when(tarificationService.calculateDetailed(
                org.mockito.Mockito.eq(TypeVehicule.DEUX_ROUES_MOTORISES),
                anyDouble(),
                any()))
                .thenReturn(new TarificationService.TarificationResult(
                        new BigDecimal("10.000"),
                        new BigDecimal("5.000"),
                        new BigDecimal("5.000"),
                        new BigDecimal("5.000"),
                        new BigDecimal("2.500"),
                        new BigDecimal("2.500"),
                        new BigDecimal("0.300"),
                        new BigDecimal("0.150"),
                        new BigDecimal("0.150"),
                        null,
                        null,
                        BigDecimal.ZERO,
                        true));

        Commande result = quoteService.prepareCommandeVehicle("cmd-route-fallback");

        assertEquals(TypeVehicule.DEUX_ROUES_MOTORISES, result.getVehicule());
        assertEquals(new BigDecimal("10.000"), result.getPrix());
        assertNotNull(result.getDistanceKm());
        org.junit.jupiter.api.Assertions.assertTrue(result.getDistanceKm() > 0);
    }

    @Test
    void sendsB2cStatusChangesToEcommerceSyncQueue() {
        EcommerceOrderStatusSyncService syncService =
                org.mockito.Mockito.mock(EcommerceOrderStatusSyncService.class);
        List<Commande.Statut> syncedStatuses = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            Commande synced = invocation.getArgument(0);
            syncedStatuses.add(synced.getStatut());
            return null;
        }).when(syncService).enqueue(any(Commande.class));
        service.setEcommerceOrderStatusSyncService(syncService);

        Commande commande = new Commande();
        commande.setId("transport-order-1");
        commande.setExternalOrderId("ecom-10-20");
        commande.setSourceCommande(Commande.SourceCommande.B2C);
        commande.setStatut(Commande.Statut.confirmer);
        when(commandeRepository.findById(commande.getId()))
                .thenReturn(java.util.Optional.of(commande));
        when(commandeRepository.save(any(Commande.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.assignerTransporteur(commande.getId(), "livreur-1");
        service.demarrerAppelClient1(commande.getId());
        service.marquerNonReponseClient1(commande.getId());
        Commande cancellation = new Commande();
        cancellation.setStatut(Commande.Statut.annulee);
        service.updateCommande(commande.getId(), cancellation);
        commande.setStatut(Commande.Statut.en_route);
        service.marquerReceptionScanne(commande.getId());

        assertEquals(
                List.of(
                        Commande.Statut.en_appelle,
                        Commande.Statut.appelle_client_1,
                        Commande.Statut.non_repondre_client_1,
                        Commande.Statut.annulee,
                        Commande.Statut.livree),
                syncedStatuses);
    }
}
