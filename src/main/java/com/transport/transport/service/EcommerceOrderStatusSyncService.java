package com.transport.transport.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.transport.model.Commande;
import com.transport.transport.model.EcommerceStatusSyncEvent;
import com.transport.transport.repository.EcommerceStatusSyncEventRepository;

@Service
public class EcommerceOrderStatusSyncService {

    private static final Logger logger = LoggerFactory.getLogger(EcommerceOrderStatusSyncService.class);
    private static final int MAX_ATTEMPTS = 12;

    private final EcommerceStatusSyncEventRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String webhookUrl;
    private final String webhookSecret;

    @Autowired
    public EcommerceOrderStatusSyncService(
            EcommerceStatusSyncEventRepository repository,
            ObjectMapper objectMapper,
            @Value("${app.ecommerce.status-webhook-url:}") String webhookUrl,
            @Value("${app.ecommerce.status-webhook-secret:}") String webhookSecret) {
        this(repository, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build(), webhookUrl, webhookSecret);
    }

    EcommerceOrderStatusSyncService(
            EcommerceStatusSyncEventRepository repository,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            String webhookUrl,
            String webhookSecret) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    public void enqueue(Commande commande) {
        String ecommerceStatus = mapStatus(commande != null ? commande.getStatut() : null);
        if (!isB2c(commande) || ecommerceStatus == null || !isConfigured()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime occurredAt = commande.getMajLe() != null ? commande.getMajLe() : now;
        String eventId = commande.getId() + ":" + ecommerceStatus + ":" + occurredAt;
        Optional<EcommerceStatusSyncEvent> existing = repository.findById(eventId);
        if (existing.map(EcommerceStatusSyncEvent::getCompletedAt).orElse(null) != null) {
            return;
        }

        EcommerceStatusSyncEvent event = existing.orElseGet(EcommerceStatusSyncEvent::new);
        event.setId(eventId);
        event.setCommandeId(commande.getId());
        event.setExternalOrderId(commande.getExternalOrderId());
        event.setTransportOrderId(commande.getId());
        event.setTransportStatus(commande.getStatut().name());
        event.setEcommerceStatus(ecommerceStatus);
        event.setOccurredAt(occurredAt);
        if (event.getCreatedAt() == null) event.setCreatedAt(now);
        event.setNextAttemptAt(now);
        repository.save(event);
    }

    @Scheduled(fixedDelayString = "${app.ecommerce.status-sync-retry-ms:5000}")
    public void retryPending() {
        if (!isConfigured()) return;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        repository
                .findTop50ByCompletedAtIsNullAndFailedPermanentlyAtIsNullAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(now)
                .forEach(this::deliver);
    }

    static String mapStatus(Commande.Statut status) {
        if (status == null) return null;
        return switch (status) {
            case confirmer -> "CONFIRMER";
            case en_appelle, en_route -> "EN_ROUTE";
            case appelle_client_1 -> "APPELLE_CLIENT_1";
            case appelle_client_2 -> "APPELLE_CLIENT_2";
            case non_repondre_client_1 -> "NON_REPONDRE_CLIENT_1";
            case non_repondre_client_2 -> "NON_REPONDRE_CLIENT_2";
            case livree -> "LIVREE";
            case annulee, ANNULEE -> "ANNULEE";
            default -> null;
        };
    }

    private void deliver(EcommerceStatusSyncEvent event) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "eventId", event.getId(),
                    "externalOrderId", event.getExternalOrderId(),
                    "transportOrderId", event.getTransportOrderId(),
                    "transportStatus", event.getTransportStatus(),
                    "status", event.getEcommerceStatus(),
                    "occurredAt", event.getOccurredAt().atOffset(ZoneOffset.UTC).toString()));
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-Transport-Webhook-Secret", webhookSecret)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            event.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
            event.setLastError(null);
            repository.save(event);
        } catch (Exception exception) {
            registerFailure(event, exception);
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void registerFailure(EcommerceStatusSyncEvent event, Exception exception) {
        int attempts = event.getAttempts() + 1;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        event.setAttempts(attempts);
        event.setLastError(truncate(exception.getMessage()));
        if (attempts >= MAX_ATTEMPTS) {
            event.setFailedPermanentlyAt(now);
            logger.error("E-commerce status sync abandoned eventId={} error={}", event.getId(), event.getLastError());
        } else {
            long delaySeconds = Math.min(1800L, 15L * (1L << Math.min(attempts - 1, 7)));
            event.setNextAttemptAt(now.plusSeconds(delaySeconds));
            logger.warn("E-commerce status sync deferred eventId={} attempt={} error={}",
                    event.getId(), attempts, event.getLastError());
        }
        repository.save(event);
    }

    private boolean isConfigured() {
        return !webhookUrl.isBlank() && !webhookSecret.isBlank();
    }

    private boolean isB2c(Commande commande) {
        return commande != null
                && commande.getSourceCommande() == Commande.SourceCommande.B2C
                && commande.getId() != null
                && commande.getExternalOrderId() != null
                && !commande.getExternalOrderId().isBlank();
    }

    private String truncate(String value) {
        if (value == null) return "Erreur inconnue";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
