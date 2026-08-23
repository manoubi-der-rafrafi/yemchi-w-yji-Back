package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.transport.model.Commande;
import com.transport.transport.model.EcommerceStatusSyncEvent;
import com.transport.transport.repository.EcommerceStatusSyncEventRepository;

class EcommerceOrderStatusSyncServiceTest {

    @Test
    void mapsTransportWorkflowToEcommerceStatuses() {
        assertEquals("CONFIRMER", EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.confirmer));
        assertEquals("EN_ROUTE", EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.en_appelle));
        assertEquals("EN_ROUTE", EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.en_route));
        assertEquals(
                "APPELLE_CLIENT_1",
                EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.appelle_client_1));
        assertEquals(
                "APPELLE_CLIENT_2",
                EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.appelle_client_2));
        assertEquals(
                "NON_REPONDRE_CLIENT_1",
                EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.non_repondre_client_1));
        assertEquals(
                "NON_REPONDRE_CLIENT_2",
                EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.non_repondre_client_2));
        assertEquals("LIVREE", EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.livree));
        assertEquals("ANNULEE", EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.annulee));
        assertEquals("ANNULEE", EcommerceOrderStatusSyncService.mapStatus(Commande.Statut.ANNULEE));
    }

    @Test
    void queuesAcceptedB2cOrderAsEnRouteWithoutCallingWebhookInline() {
        EcommerceStatusSyncEventRepository repository =
                mock(EcommerceStatusSyncEventRepository.class);
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        EcommerceOrderStatusSyncService service = new EcommerceOrderStatusSyncService(
                repository,
                new ObjectMapper(),
                mock(HttpClient.class),
                "https://ecommerce.example/api/internal/transport/order-status",
                "shared-secret");
        Commande commande = new Commande();
        commande.setId("transport-1");
        commande.setExternalOrderId("ecom-1-10");
        commande.setSourceCommande(Commande.SourceCommande.B2C);
        commande.setStatut(Commande.Statut.en_appelle);
        commande.setMajLe(LocalDateTime.of(2026, 8, 17, 10, 0));

        service.enqueue(commande);

        ArgumentCaptor<EcommerceStatusSyncEvent> captor =
                ArgumentCaptor.forClass(EcommerceStatusSyncEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("EN_ROUTE", captor.getValue().getEcommerceStatus());
        assertEquals("ecom-1-10", captor.getValue().getExternalOrderId());
    }
}
