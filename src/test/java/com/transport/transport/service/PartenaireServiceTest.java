package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Partenaire;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.PartenaireRepository;

class PartenaireServiceTest {

    @Test
    void updatesPartnerAndActiveB2cCommandsWithCurrentContactProfile() {
        PartenaireRepository partenaireRepository = mock(PartenaireRepository.class);
        CommandeRepository commandeRepository = mock(CommandeRepository.class);
        Partenaire partenaire = new Partenaire();
        partenaire.setId("partner-1");
        partenaire.setExternalBusinessId("ma-boutique");
        Commande active = new Commande();
        active.setExternalBusinessId("ma-boutique");
        active.setSourceCommande(Commande.SourceCommande.B2C);
        active.setStatut(Commande.Statut.en_route);

        when(partenaireRepository.findByExternalBusinessId("ma-boutique"))
                .thenReturn(Optional.of(partenaire));
        when(partenaireRepository.save(any(Partenaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(commandeRepository.findByExternalBusinessIdAndSourceCommandeAndStatutNotIn(
                eq("ma-boutique"), eq(Commande.SourceCommande.B2C), any()))
                .thenReturn(List.of(active));

        PartenaireService service = new PartenaireService(
                partenaireRepository,
                commandeRepository,
                mock(PartnerApiKeyService.class));

        Partenaire result = service.updatePartnerProfile(
                "ma-boutique",
                "Ma Boutique",
                "https://cdn.example/logo.png",
                "+216 20 000 000",
                List.of("+216 20 000 000", "+216 21 000 000"),
                "contact@example.com",
                "10 rue de Tunis",
                "https://facebook.com/ma-boutique",
                "https://instagram.com/ma-boutique",
                null,
                36.8065,
                10.1815);

        assertEquals("Ma Boutique", result.getBusinessName());
        assertEquals("contact@example.com", result.getEmail());
        assertEquals(2, result.getPhoneNumbers().size());
        assertEquals("Ma Boutique", active.getPartenaireNom());
        assertEquals("https://cdn.example/logo.png", active.getPartenaireLogoUrl());
        assertEquals("+216 20 000 000", active.getTelDepart());
        assertEquals("10 rue de Tunis", active.getLocalisationDepart());
        assertEquals(36.8065, active.getLatitudeDepart());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Commande.Statut>> excludedStatuses = ArgumentCaptor.forClass(List.class);
        verify(commandeRepository).findByExternalBusinessIdAndSourceCommandeAndStatutNotIn(
                eq("ma-boutique"),
                eq(Commande.SourceCommande.B2C),
                excludedStatuses.capture());
        assertEquals(
                List.of(Commande.Statut.livree, Commande.Statut.ANNULEE, Commande.Statut.annulee),
                excludedStatuses.getValue());
        verify(commandeRepository).saveAll(List.of(active));
    }

    @Test
    void doesNotWriteCommandsWhenThereAreNoActiveB2cOrders() {
        PartenaireRepository partenaireRepository = mock(PartenaireRepository.class);
        CommandeRepository commandeRepository = mock(CommandeRepository.class);
        Partenaire partenaire = new Partenaire();
        partenaire.setExternalBusinessId("ma-boutique");
        when(partenaireRepository.findByExternalBusinessId("ma-boutique"))
                .thenReturn(Optional.of(partenaire));
        when(partenaireRepository.save(any(Partenaire.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(commandeRepository.findByExternalBusinessIdAndSourceCommandeAndStatutNotIn(
                eq("ma-boutique"), eq(Commande.SourceCommande.B2C), any()))
                .thenReturn(List.of());

        PartenaireService service = new PartenaireService(
                partenaireRepository,
                commandeRepository,
                mock(PartnerApiKeyService.class));
        service.updatePartnerProfile(
                "ma-boutique", "Ma Boutique", null, null, List.of(), null, null,
                null, null, null, null, null);

        verify(commandeRepository, never()).saveAll(any());
    }
}
