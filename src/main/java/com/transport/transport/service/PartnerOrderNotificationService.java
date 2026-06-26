package com.transport.transport.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.dto.partner.PartnerCommandeResponse;
import com.transport.transport.dto.partner.PartnerCreateCommandeRequest;
import com.transport.transport.security.PartnerPrincipal;

@Service
public class PartnerOrderNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PartnerOrderNotificationService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MailService mailService;
    private final String alertEmail;

    public PartnerOrderNotificationService(
            MailService mailService,
            @Value("${app.partner-order.alert-email:manoubi.rafrafi109@gmail.com}") String alertEmail) {
        this.mailService = mailService;
        this.alertEmail = alertEmail != null ? alertEmail.trim() : "";
    }

    public void notifySuccess(
            PartnerPrincipal principal,
            PartnerCreateCommandeRequest request,
            PartnerCommandeResponse response) {
        if (alertEmail.isBlank()) {
            return;
        }

        String subject = "Succes ajout commande partenaire";
        String body = buildCommonSection(principal, request)
                + "Resultat: SUCCES\n"
                + "Commande transport id: " + safe(response != null && response.commande() != null ? response.commande().getId() : null) + "\n"
                + "Statut transport: " + safe(response != null && response.commande() != null && response.commande().getStatut() != null
                        ? response.commande().getStatut().name()
                        : null) + "\n"
                + "Produits crees: " + (response != null && response.produits() != null ? response.produits().size() : 0) + "\n";

        send(subject, body);
    }

    public void notifyFailure(
            PartnerPrincipal principal,
            PartnerCreateCommandeRequest request,
            Exception exception) {
        if (alertEmail.isBlank()) {
            return;
        }

        String subject = "Echec ajout commande partenaire";
        StringBuilder body = new StringBuilder(buildCommonSection(principal, request));
        body.append("Resultat: ECHEC\n");
        body.append("Type erreur: ").append(exception != null ? exception.getClass().getSimpleName() : "(inconnue)").append("\n");
        body.append("Message: ").append(exception != null && exception.getMessage() != null ? exception.getMessage() : "(vide)").append("\n");

        if (exception instanceof ResponseStatusException responseStatusException) {
            HttpStatus status = HttpStatus.resolve(responseStatusException.getStatusCode().value());
            body.append("Http status: ")
                    .append(status != null ? status.value() + " " + status.name() : responseStatusException.getStatusCode().value())
                    .append("\n");
        }

        send(subject, body.toString());
    }

    private String buildCommonSection(
            PartnerPrincipal principal,
            PartnerCreateCommandeRequest request) {
        PartnerCreateCommandeRequest.ContactPoint depart = request != null ? request.depart() : null;
        PartnerCreateCommandeRequest.ContactPoint arrivee = request != null ? request.arrivee() : null;
        List<PartnerCreateCommandeRequest.ProductItem> produits = request != null ? request.produits() : null;

        return new StringBuilder()
                .append("Date: ").append(LocalDateTime.now().format(DATE_TIME_FORMATTER)).append("\n")
                .append("Business name: ").append(safe(principal != null ? principal.getBusinessName() : null)).append("\n")
                .append("Partner id: ").append(safe(principal != null ? principal.getPartnerId() : null)).append("\n")
                .append("External business id: ").append(safe(principal != null ? principal.getExternalBusinessId() : null)).append("\n")
                .append("External order id: ").append(safe(request != null ? request.externalOrderId() : null)).append("\n")
                .append("Prix: ").append(formatPrice(request != null ? request.prix() : null)).append("\n")
                .append("Mode paiement: ").append(safe(request != null && request.modePaiement() != null ? request.modePaiement().name() : null)).append("\n")
                .append("Depart: ").append(formatPoint(depart)).append("\n")
                .append("Arrivee: ").append(formatPoint(arrivee)).append("\n")
                .append("Nombre produits: ").append(produits != null ? produits.size() : 0).append("\n\n")
                .toString();
    }

    private String formatPoint(PartnerCreateCommandeRequest.ContactPoint point) {
        if (point == null) {
            return "(absent)";
        }

        return safe(point.nom())
                + " | tel=" + safe(point.telephone())
                + " | adresse=" + safe(point.adresse())
                + " | lat=" + safe(point.latitude())
                + " | lng=" + safe(point.longitude());
    }

    private String formatPrice(BigDecimal value) {
        return value != null ? value.stripTrailingZeros().toPlainString() : "(vide)";
    }

    private String safe(Object value) {
        return value != null ? String.valueOf(value) : "(vide)";
    }

    private void send(String subject, String body) {
        try {
            mailService.sendTextEmail(alertEmail, subject, body);
        } catch (RuntimeException exception) {
            logger.error("Unable to send partner order notification email", exception);
        }
    }
}
