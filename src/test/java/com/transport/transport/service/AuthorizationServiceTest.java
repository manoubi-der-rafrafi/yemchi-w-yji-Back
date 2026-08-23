package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.AmiRepository;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.UtilisateurRepository;

class AuthorizationServiceTest {

    private AuthorizationService service;
    private Utilisateur transporteur;
    private UtilisateurRepository utilisateurRepository;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        service = new AuthorizationService(
                utilisateurRepository,
                mock(AmiRepository.class),
                mock(CommandeRepository.class));
        transporteur = new Utilisateur();
        transporteur.setId("livreur-1");
        transporteur.setRole(Utilisateur.Role.transporteur);
        when(utilisateurRepository.findByEmailIgnoreCase("livreur@example.com"))
                .thenReturn(Optional.of(transporteur));
    }

    @Test
    void transporteurCanReadProductsOfConfirmedUnassignedCommande() {
        Commande commande = new Commande();
        commande.setStatut(Commande.Statut.confirmer);

        assertDoesNotThrow(() -> service.requireCommandeProductsReadAccess(
                commande,
                authentication()));
        assertFalse(service.canAccessCommande(transporteur, commande));
    }

    @Test
    void transporteurCannotReadProductsOfCommandeAssignedToSomeoneElse() {
        Commande commande = new Commande();
        commande.setStatut(Commande.Statut.confirmer);
        commande.setTransporteurId("livreur-2");

        assertThrows(
                ResponseStatusException.class,
                () -> service.requireCommandeProductsReadAccess(commande, authentication()));
    }

    @Test
    void clientCannotReadProductsOfUnrelatedAvailableCommande() {
        Utilisateur client = new Utilisateur();
        client.setId("client-1");
        client.setRole(Utilisateur.Role.client);
        Commande commande = new Commande();
        commande.setStatut(Commande.Statut.confirmer);

        assertFalse(service.canAccessCommande(client, commande));
    }

    @Test
    void clientOwnerCanMutateOwnCommande() {
        Utilisateur client = utilisateur("client-1", Utilisateur.Role.client);
        when(utilisateurRepository.findByEmailIgnoreCase("client@example.com"))
                .thenReturn(Optional.of(client));
        Commande commande = new Commande();
        commande.setClientId("client-1");

        assertDoesNotThrow(() -> service.requireCommandeOwnerOrAdmin(
                commande, authentication("client@example.com")));
    }

    @Test
    void friendCannotMutateCommandeOfAnotherClient() {
        Utilisateur friend = utilisateur("friend-1", Utilisateur.Role.client);
        when(utilisateurRepository.findByEmailIgnoreCase("friend@example.com"))
                .thenReturn(Optional.of(friend));
        Commande commande = new Commande();
        commande.setClientId("client-1");
        commande.setIdAmie("friend-1");

        assertThrows(ResponseStatusException.class, () -> service.requireCommandeOwnerOrAdmin(
                commande, authentication("friend@example.com")));
    }

    @Test
    void assignedTransporteurCanPerformOperationalActions() {
        Commande commande = new Commande();
        commande.setClientId("client-1");
        commande.setTransporteurId("livreur-1");

        assertDoesNotThrow(() -> service.requireAssignedTransporteurOrAdmin(
                commande, authentication()));
    }

    @Test
    void clientCannotPerformTransporteurOperationalActions() {
        Utilisateur client = utilisateur("client-1", Utilisateur.Role.client);
        when(utilisateurRepository.findByEmailIgnoreCase("client@example.com"))
                .thenReturn(Optional.of(client));
        Commande commande = new Commande();
        commande.setClientId("client-1");
        commande.setTransporteurId("livreur-1");

        assertThrows(ResponseStatusException.class, () -> service.requireAssignedTransporteurOrAdmin(
                commande, authentication("client@example.com")));
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return authentication("livreur@example.com");
    }

    private UsernamePasswordAuthenticationToken authentication(String email) {
        return UsernamePasswordAuthenticationToken.authenticated(
                email,
                "token",
                java.util.List.of());
    }

    private Utilisateur utilisateur(String id, Utilisateur.Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setRole(role);
        return utilisateur;
    }
}
