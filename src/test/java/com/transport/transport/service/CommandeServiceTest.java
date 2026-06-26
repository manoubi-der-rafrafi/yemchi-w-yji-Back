package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.ProduitRepository;
import com.transport.transport.repository.UtilisateurRepository;

class CommandeServiceTest {

    private CommandeRepository commandeRepository;
    private ProduitRepository produitRepository;
    private UtilisateurRepository utilisateurRepository;
    private VehicleAnalysisService vehicleAnalysisService;
    private CommandeService service;

    @BeforeEach
    void setUp() {
        commandeRepository = org.mockito.Mockito.mock(CommandeRepository.class);
        produitRepository = org.mockito.Mockito.mock(ProduitRepository.class);
        utilisateurRepository = org.mockito.Mockito.mock(UtilisateurRepository.class);
        vehicleAnalysisService = org.mockito.Mockito.mock(VehicleAnalysisService.class);
        service = new CommandeService(
                commandeRepository,
                produitRepository,
                utilisateurRepository,
                vehicleAnalysisService);
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
}
