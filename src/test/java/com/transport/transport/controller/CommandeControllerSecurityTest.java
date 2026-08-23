package com.transport.transport.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.CommandeService;

class CommandeControllerSecurityTest {
    private CommandeController controller;
    private CommandeService commandes;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new CommandeController();
        commandes = mock(CommandeService.class);
        AuthorizationService authorization = mock(AuthorizationService.class);
        authentication = mock(Authentication.class);

        Utilisateur client = new Utilisateur();
        client.setId("client-1");
        client.setRole(Utilisateur.Role.client);
        when(authorization.currentUser(authentication)).thenReturn(client);
        when(authorization.isAdmin(client)).thenReturn(false);
        ReflectionTestUtils.setField(controller, "commandeService", commandes);
        ReflectionTestUtils.setField(controller, "authorizationService", authorization);
    }

    @Test
    void creationIgnoresIdsStatusAndSettlementFieldsChosenByClient() {
        Commande payload = maliciousPayload();
        payload.setId("forced-id");
        payload.setClientId("client-1");
        when(commandes.createCommande(any(Commande.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Commande result = controller.createCommande(payload, authentication);

        assertNull(result.getId());
        assertEquals("client-1", result.getClientId());
        assertEquals(Commande.Statut.en_cours, result.getStatut());
        assertEquals(Commande.SourceCommande.C2C, result.getSourceCommande());
        assertSensitiveFieldsCleared(result);
    }

    @Test
    void genericUpdateCannotPerformBusinessStateTransition() {
        Commande existing = new Commande();
        existing.setId("order-1");
        existing.setClientId("client-1");
        Commande payload = maliciousPayload();
        payload.setInstructions("Laisser a l'accueil");
        when(commandes.getCommandeById("order-1")).thenReturn(Optional.of(existing));
        when(commandes.updateCommande("order-1", payload)).thenReturn(payload);

        Commande result = controller.updateCommande("order-1", payload, authentication).getBody();

        assertNull(result.getStatut());
        assertNull(result.getSourceCommande());
        assertEquals("Laisser a l'accueil", result.getInstructions());
        assertSensitiveFieldsCleared(result);
    }

    private static Commande maliciousPayload() {
        Commande payload = new Commande();
        payload.setClientId("victim");
        payload.setTransporteurId("chosen-driver");
        payload.setTransporteurSecoursId("chosen-backup");
        payload.setPartenaireId("partner");
        payload.setExternalOrderId("external-1");
        payload.setStatut(Commande.Statut.livree);
        payload.setSourceCommande(Commande.SourceCommande.B2C);
        payload.setStatutReglement(Commande.StatutReglement.REGLE);
        payload.setPrixTotalClient(new BigDecimal("0.01"));
        payload.setQrCodeDepartScanne(true);
        payload.setQrCodeReceptionScanne(true);
        return payload;
    }

    private static void assertSensitiveFieldsCleared(Commande result) {
        assertNull(result.getTransporteurId());
        assertNull(result.getTransporteurSecoursId());
        assertNull(result.getPartenaireId());
        assertNull(result.getExternalOrderId());
        assertNull(result.getStatutReglement());
        assertNull(result.getPrixTotalClient());
        assertFalse(result.isQrCodeDepartScanne());
        assertFalse(result.isQrCodeReceptionScanne());
    }
}
