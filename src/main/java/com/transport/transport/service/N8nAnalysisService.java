package com.transport.transport.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

@Service
public class N8nAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(N8nAnalysisService.class);
    private static final int MAX_IMAGE_URL_LENGTH = 2_048;
    private static final int MAX_PROMPT_LENGTH = 4_000;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI imageAnalysisUri;
    private final URI orderAnalysisUri;
    private final String webhookSecret;
    private final Duration requestTimeout;
    private final Set<String> allowedImageHosts;

    public N8nAnalysisService(
            ObjectMapper objectMapper,
            @Value("${app.n8n.image-analysis-url:}") String imageAnalysisUrl,
            @Value("${app.n8n.order-analysis-url:}") String orderAnalysisUrl,
            @Value("${app.n8n.webhook-secret:}") String webhookSecret,
            @Value("${app.n8n.timeout-ms:10000}") long timeoutMs,
            @Value("${app.n8n.allowed-image-hosts:res.cloudinary.com}") String allowedImageHosts,
            @Value("${app.n8n.allowed-http-hosts:localhost,127.0.0.1,workflow}") String allowedHttpHosts) {
        this.objectMapper = objectMapper;
        this.requestTimeout = Duration.ofMillis(Math.max(1_000L, timeoutMs));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.requestTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        Set<String> normalizedAllowedHttpHosts = Arrays.stream(allowedHttpHosts.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.imageAnalysisUri = parseWebhookUri(imageAnalysisUrl, "image", normalizedAllowedHttpHosts);
        this.orderAnalysisUri = parseWebhookUri(orderAnalysisUrl, "commande", normalizedAllowedHttpHosts);
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        if ((this.imageAnalysisUri != null || this.orderAnalysisUri != null)
                && this.webhookSecret.isBlank()) {
            throw new IllegalStateException(
                    "N8N_WEBHOOK_SECRET est obligatoire quand un webhook n8n est configure");
        }
        this.allowedImageHosts = Arrays.stream(allowedImageHosts.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public JsonNode analyzeImage(String imageUrl) {
        String validatedUrl = validateImageUrl(imageUrl);
        return callWebhook(imageAnalysisUri, Map.of("image", validatedUrl), "image");
    }

    public JsonNode analyzeOrder(String prompt) {
        String normalized = prompt == null ? "" : prompt.trim();
        if (normalized.isBlank() || normalized.length() > MAX_PROMPT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prompt invalide");
        }
        return callWebhook(orderAnalysisUri, Map.of("prompt", normalized), "commande");
    }

    private JsonNode callWebhook(URI endpoint, Object payload, String operation) {
        if (endpoint == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service d'analyse non configure");
        }

        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(requestTimeout)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8));
            if (!webhookSecret.isBlank()) {
                request.header("X-Webhook-Secret", webhookSecret);
            }

            HttpResponse<String> response = httpClient.send(
                    request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("n8n {} analysis failed with status {}", operation, response.statusCode());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service d'analyse indisponible");
            }

            String body = response.body() == null ? "" : response.body().trim();
            if (body.isEmpty()) {
                return objectMapper.createObjectNode();
            }
            try {
                return objectMapper.readTree(body);
            } catch (IOException invalidJson) {
                return TextNode.valueOf(body);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Analyse interrompue");
        } catch (IOException exception) {
            logger.warn("n8n {} analysis network failure: {}", operation, exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service d'analyse indisponible");
        }
    }

    private String validateImageUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > MAX_IMAGE_URL_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL d'image invalide");
        }

        try {
            URI uri = new URI(normalized);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean allowedHost = allowedImageHosts.stream()
                    .anyMatch(allowed -> host.equals(allowed) || host.endsWith('.' + allowed));
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || !allowedHost) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL d'image non autorisee");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL d'image invalide");
        }
    }

    private static URI parseWebhookUri(String value, String operation, Set<String> allowedHttpHosts) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = new URI(value.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean allowedInternalHttpHost = allowedHttpHosts.contains(host);
            if (host.isBlank() || uri.getUserInfo() != null ||
                    !("https".equalsIgnoreCase(uri.getScheme()) ||
                            (allowedInternalHttpHost && "http".equalsIgnoreCase(uri.getScheme())))) {
                throw new IllegalArgumentException("URL n8n " + operation + " non securisee");
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("URL n8n " + operation + " invalide", exception);
        }
    }
}
