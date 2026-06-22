package com.transport.transport.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transport.transport.dto.partner.PartnerCreateCommandeRequest;
import com.transport.transport.model.Produit;
import com.transport.transport.model.TypeVehicule;

@Service
public class VehicleAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleAnalysisService.class);

    private final MailService mailService;
    private final ObjectMapper objectMapper;
    private final String vehicleAnalysisUrl;
    private final String vehicleAnalysisAlertEmail;
    private final HttpClient httpClient;
    private final Duration vehicleAnalysisTimeout;

    public VehicleAnalysisService(
            MailService mailService,
            ObjectMapper objectMapper,
            @Value("${app.vehicle-analysis.url:}") String vehicleAnalysisUrl,
            @Value("${app.vehicle-analysis.alert-email:manoubi.rafrafi109@gmail.com}") String vehicleAnalysisAlertEmail,
            @Value("${app.vehicle-analysis.timeout-ms:8000}") long vehicleAnalysisTimeoutMs) {
        this.mailService = mailService;
        this.objectMapper = objectMapper;
        this.vehicleAnalysisUrl = vehicleAnalysisUrl != null ? vehicleAnalysisUrl.trim() : "";
        this.vehicleAnalysisAlertEmail = vehicleAnalysisAlertEmail != null ? vehicleAnalysisAlertEmail.trim() : "";
        this.vehicleAnalysisTimeout = Duration.ofMillis(Math.max(vehicleAnalysisTimeoutMs, 1000L));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(this.vehicleAnalysisTimeout)
                .build();
    }

    public TypeVehicule resolveVehicleForProduits(List<Produit> produits) {
        List<VehicleItem> items = (produits == null ? List.<Produit>of() : produits).stream()
                .map(produit -> new VehicleItem(
                        produit.getNom(),
                        produit.getType(),
                        produit.getQuantite(),
                        produit.getPoids(),
                        produit.getLargeur(),
                        produit.getProfondeur(),
                        produit.getHauteur()))
                .toList();
        return resolveVehicle(items);
    }

    public TypeVehicule resolveVehicleForPartnerProducts(List<PartnerCreateCommandeRequest.ProductItem> produits) {
        List<VehicleItem> items = (produits == null ? List.<PartnerCreateCommandeRequest.ProductItem>of() : produits).stream()
                .map(produit -> new VehicleItem(
                        produit.nom(),
                        produit.type(),
                        produit.quantite(),
                        produit.poids(),
                        produit.largeur(),
                        produit.profondeur(),
                        produit.hauteur()))
                .toList();
        return resolveVehicle(items);
    }

    private TypeVehicule resolveVehicle(List<VehicleItem> items) {
        TypeVehicule externalVehicle = analyzeVehicleExternally(items);
        if (externalVehicle != null) {
            return externalVehicle;
        }
        TypeVehicule fallbackVehicle = estimateVehicleLocally(items);
        logger.info("Vehicle analysis fallback selected vehicle={}", fallbackVehicle);
        return fallbackVehicle;
    }

    private TypeVehicule analyzeVehicleExternally(List<VehicleItem> items) {
        if (vehicleAnalysisUrl.isBlank() || items == null || items.isEmpty()) {
            notifyVehicleAnalysisFailure(
                    "Configuration manquante pour l'analyse vehicule",
                    null,
                    items,
                    null);
            return null;
        }

        String prompt = buildOrderAnalysisPrompt(items);

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of("prompt", prompt));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(vehicleAnalysisUrl))
                    .timeout(vehicleAnalysisTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("Vehicle analysis webhook returned status {}", response.statusCode());
                notifyVehicleAnalysisFailure(
                        "Le webhook d'analyse vehicule a retourne un statut HTTP non valide",
                        prompt,
                        items,
                        "status=" + response.statusCode() + ", body=" + response.body());
                return null;
            }

            TypeVehicule selectedVehicle = extractVehicleFromAnalysisResponse(response.body());
            if (selectedVehicle == null) {
                logger.warn("Vehicle analysis webhook returned no valid vehicle");
                notifyVehicleAnalysisFailure(
                        "Le webhook d'analyse vehicule n'a retourne aucun vehicule exploitable",
                        prompt,
                        items,
                        response.body());
            }
            return selectedVehicle;
        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warn("Vehicle analysis webhook failed: {}", exception.getMessage());
            notifyVehicleAnalysisFailure(
                    "Erreur d'appel du webhook d'analyse vehicule",
                    prompt,
                    items,
                    exception.toString());
            return null;
        }
    }

    private void notifyVehicleAnalysisFailure(
            String summary,
            String prompt,
            List<VehicleItem> items,
            String rawError) {
        if (vehicleAnalysisAlertEmail.isBlank()) {
            return;
        }

        StringBuilder body = new StringBuilder();
        body.append("Une erreur est survenue lors de l'estimation du vehicule.\n\n");
        body.append("Resume: ").append(summary).append("\n");
        body.append("Webhook: ").append(vehicleAnalysisUrl.isBlank() ? "(vide)" : vehicleAnalysisUrl).append("\n");
        body.append("TimeoutMs: ").append(vehicleAnalysisTimeout.toMillis()).append("\n");
        body.append("Erreur brute: ").append(rawError != null ? rawError : "(aucune)").append("\n\n");
        body.append("Produits:\n");

        if (items == null || items.isEmpty()) {
            body.append("- Aucun produit fourni\n");
        } else {
            for (VehicleItem item : items) {
                body.append(formatItemForOrderAnalysis(item)).append("\n\n");
            }
        }

        body.append("Prompt:\n").append(prompt != null ? prompt : "(non genere)").append("\n");

        try {
            mailService.sendTextEmail(
                    vehicleAnalysisAlertEmail,
                    "Alerte estimation vehicule transport",
                    body.toString());
        } catch (RuntimeException exception) {
            logger.error("Unable to send vehicle analysis failure email", exception);
        }
    }

    private String buildOrderAnalysisPrompt(List<VehicleItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("Analyze this delivery order and choose the most appropriate vehicle.\n");
        builder.append("Return JSON only with keys \"thinking\" and \"selected_vehicle\".\n");
        builder.append("Products:\n");

        for (VehicleItem item : items) {
            builder.append(formatItemForOrderAnalysis(item)).append("\n\n");
        }

        return builder.toString().trim();
    }

    private String formatItemForOrderAnalysis(VehicleItem item) {
        StringBuilder builder = new StringBuilder();
        String displayName = item.name() != null && !item.name().isBlank() ? item.name().trim() : "Unknown product";
        builder.append("- ").append(displayName);

        if (item.type() != null && !item.type().isBlank()) {
            builder.append("\n  Category: ").append(item.type().trim());
        }

        String dimensions = formatDimensions(item);
        if (dimensions != null) {
            builder.append("\n  Dimensions: ").append(dimensions);
        }

        Integer quantite = item.quantite();
        if (quantite != null && quantite > 1) {
            builder.append("\n  Quantity: ").append(quantite);
        }

        if (item.poids() != null) {
            builder.append("\n  WeightKg: ").append(item.poids().stripTrailingZeros().toPlainString());
        }

        return builder.toString();
    }

    private String formatDimensions(VehicleItem item) {
        List<String> parts = new ArrayList<>();

        if (item.hauteur() != null) {
            parts.add(item.hauteur().stripTrailingZeros().toPlainString() + "cm (H)");
        }
        if (item.largeur() != null) {
            parts.add(item.largeur().stripTrailingZeros().toPlainString() + "cm (W)");
        }
        if (item.profondeur() != null) {
            parts.add(item.profondeur().stripTrailingZeros().toPlainString() + "cm (D)");
        }

        return parts.isEmpty() ? null : String.join(" x ", parts);
    }

    private TypeVehicule extractVehicleFromAnalysisResponse(String responseBody) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        JsonNode root = tryParseJson(responseBody);
        if (root != null) {
            String selectedVehicle = extractSelectedVehicle(root);
            TypeVehicule mappedVehicle = mapSelectedVehicle(selectedVehicle);
            if (mappedVehicle != null) {
                return mappedVehicle;
            }
        }

        return mapSelectedVehicle(responseBody);
    }

    private JsonNode tryParseJson(String text) throws IOException {
        String trimmed = stripCodeFence(text);
        if (trimmed.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(trimmed);
        } catch (IOException firstError) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return objectMapper.readTree(trimmed.substring(start, end + 1));
            }
            throw firstError;
        }
    }

    private String stripCodeFence(String text) {
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            String withoutStart = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            return withoutStart.replaceFirst("\\s*```$", "").trim();
        }
        return trimmed;
    }

    private String extractSelectedVehicle(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText("");
        }

        JsonNode direct = firstNonBlank(
                node.get("selected_vehicle"),
                node.get("selectedVehicle"),
                node.get("selected_vehicle_name"),
                node.get("vehicle"));
        if (direct != null && direct.isTextual()) {
            return direct.asText("");
        }

        JsonNode textNode = firstNonBlank(node.get("text"), node.get("thinking"));
        if (textNode != null && textNode.isTextual()) {
            return textNode.asText("");
        }

        if (node.has("content") && node.get("content").isArray()) {
            for (JsonNode child : node.get("content")) {
                String nested = extractSelectedVehicle(child);
                if (!nested.isBlank()) {
                    return nested;
                }
            }
        }

        if (node.has("output") && node.get("output").isArray()) {
            for (JsonNode child : node.get("output")) {
                String nested = extractSelectedVehicle(child);
                if (!nested.isBlank()) {
                    return nested;
                }
            }
        }

        return "";
    }

    private JsonNode firstNonBlank(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node != null && !node.isNull() && !(node.isTextual() && node.asText("").isBlank())) {
                return node;
            }
        }
        return null;
    }

    private TypeVehicule mapSelectedVehicle(String rawValue) {
        String normalized = rawValue != null ? rawValue.trim().toLowerCase() : "";
        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.contains("moto")
                || normalized.contains("motorcycle")
                || normalized.contains("two wheel")
                || normalized.contains("deux roues")) {
            return TypeVehicule.DEUX_ROUES_MOTORISES;
        }
        if (normalized.contains("light commercial")
                || normalized.contains("utility light")
                || normalized.contains("vehicule utilitaire leger")) {
            return TypeVehicule.VEHICULE_UTILITAIRE_LEGER;
        }
        if (normalized.contains("fourgon")
                || normalized.contains("minibus")
                || normalized.contains("van")) {
            return TypeVehicule.FOURGON_MINIBUS;
        }
        if (normalized.contains("gros utilitaire")
                || normalized.contains("truck")
                || normalized.contains("heavy utility")) {
            return TypeVehicule.GROS_UTILITAIRE;
        }
        if (normalized.contains("vehicule particulier")
                || normalized.contains("passenger car")
                || normalized.contains("private car")
                || normalized.contains("car")) {
            return TypeVehicule.VEHICULE_PARTICULIER;
        }

        return null;
    }

    private TypeVehicule estimateVehicleLocally(List<VehicleItem> items) {
        BigDecimal totalPoids = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;

        for (VehicleItem item : items == null ? List.<VehicleItem>of() : items) {
            int quantite = item.quantite() != null && item.quantite() > 0 ? item.quantite() : 1;

            if (item.poids() != null) {
                totalPoids = totalPoids.add(item.poids().multiply(BigDecimal.valueOf(quantite)));
            }

            if (item.largeur() != null && item.profondeur() != null && item.hauteur() != null) {
                BigDecimal volume = item.largeur()
                        .multiply(item.profondeur())
                        .multiply(item.hauteur())
                        .multiply(BigDecimal.valueOf(quantite))
                        .divide(BigDecimal.valueOf(1_000_000), 3, RoundingMode.HALF_UP);
                totalVolume = totalVolume.add(volume);
            }
        }

        if (lte(totalPoids, "15") && lte(totalVolume, "0.125")) {
            return TypeVehicule.DEUX_ROUES_MOTORISES;
        }
        if (lte(totalPoids, "80") && lte(totalVolume, "1.000")) {
            return TypeVehicule.VEHICULE_PARTICULIER;
        }
        if (lte(totalPoids, "300") && lte(totalVolume, "3.000")) {
            return TypeVehicule.VEHICULE_UTILITAIRE_LEGER;
        }
        if (lte(totalPoids, "800") && lte(totalVolume, "8.000")) {
            return TypeVehicule.FOURGON_MINIBUS;
        }
        return TypeVehicule.GROS_UTILITAIRE;
    }

    private boolean lte(BigDecimal value, String limit) {
        return value.compareTo(new BigDecimal(limit)) <= 0;
    }

    private record VehicleItem(
            String name,
            String type,
            Integer quantite,
            BigDecimal poids,
            BigDecimal largeur,
            BigDecimal profondeur,
            BigDecimal hauteur) {}
}
