package com.transport.transport.service;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.dto.partner.PartnerCommandeResponse;
import com.transport.transport.dto.partner.PartnerCreateCommandeRequest;
import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;
import com.transport.transport.security.PartnerPrincipal;

class PartnerOrderNotificationServiceTest {

    private MailService mailService;
    private PartnerOrderNotificationService service;

    @BeforeEach
    void setUp() {
        mailService = org.mockito.Mockito.mock(MailService.class);
        service = new PartnerOrderNotificationService(mailService, "manoubi.rafrafi109@gmail.com");
    }

    @Test
    void notifySuccessSendsEmail() {
        PartnerPrincipal principal = new PartnerPrincipal(
                "partner-1",
                "maison-cerisette",
                "Maison Cerisette",
                Set.of("delivery:create"),
                "pk_live_test");
        PartnerCreateCommandeRequest request = buildRequest();
        Commande commande = new Commande();
        commande.setId("cmd-1");
        commande.setStatut(Commande.Statut.confirmer);
        PartnerCommandeResponse response = new PartnerCommandeResponse(commande, List.of(new Produit()));

        service.notifySuccess(principal, request, response);

        verify(mailService).sendTextEmail(
                eq("manoubi.rafrafi109@gmail.com"),
                eq("Succes ajout commande partenaire"),
                contains("Resultat: SUCCES"));
    }

    @Test
    void notifyFailureSendsEmail() {
        PartnerPrincipal principal = new PartnerPrincipal(
                "partner-1",
                "maison-cerisette",
                "Maison Cerisette",
                Set.of("delivery:create"),
                "pk_live_test");
        PartnerCreateCommandeRequest request = buildRequest();

        service.notifyFailure(principal, request, new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "externalOrderId deja utilise"));

        verify(mailService).sendTextEmail(
                eq("manoubi.rafrafi109@gmail.com"),
                eq("Echec ajout commande partenaire"),
                contains("Resultat: ECHEC"));
    }

    private PartnerCreateCommandeRequest buildRequest() {
        return new PartnerCreateCommandeRequest(
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
    }
}
