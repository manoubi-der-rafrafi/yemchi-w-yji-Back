package com.transport.transport.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RoutingService {

    private static final URI ORS_DIRECTIONS_URI =
            URI.create("https://api.openrouteservice.org/v2/directions/driving-car");
    private static final double FALLBACK_ROAD_FACTOR = 1.25;
    private static final double FALLBACK_AVERAGE_SPEED_KMH = 30.0;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String orsApiKey;

    @Autowired
    public RoutingService(ObjectMapper objectMapper, @Value("${ors.api.key:}") String orsApiKey) {
        this(objectMapper, orsApiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    RoutingService(ObjectMapper objectMapper, String orsApiKey, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.orsApiKey = orsApiKey;
        this.httpClient = httpClient;
    }

    public RouteResult calculateRoute(Double lat1, Double lon1, Double lat2, Double lon2) {
        validateCoordinates(lat1, lon1, lat2, lon2);
        if (orsApiKey == null || orsApiKey.isBlank()) {
            return fallbackRoute(lat1, lon1, lat2, lon2);
        }

        URI uri = URI.create(ORS_DIRECTIONS_URI + "?start="
                + encode(lon1 + "," + lat1)
                + "&end="
                + encode(lon2 + "," + lat2));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", orsApiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return fallbackRoute(lat1, lon1, lat2, lon2);
            }
            return parseRoute(response.body());
        } catch (IOException e) {
            return fallbackRoute(lat1, lon1, lat2, lon2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallbackRoute(lat1, lon1, lat2, lon2);
        }
    }

    private RouteResult fallbackRoute(Double lat1, Double lon1, Double lat2, Double lon2) {
        double straightKm = haversineKm(lat1, lon1, lat2, lon2);
        double roadKm = Math.max(0.001, straightKm * FALLBACK_ROAD_FACTOR);
        double roundedKm = Math.round(roadKm * 1000.0) / 1000.0;
        long minutes = Math.max(1L, Math.round((roundedKm / FALLBACK_AVERAGE_SPEED_KMH) * 60.0));
        return new RouteResult(roundedKm, minutes, List.of());
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private RouteResult parseRoute(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode feature = root.path("features").path(0);
        JsonNode summary = feature.path("properties").path("summary");
        JsonNode coordinatesNode = feature.path("geometry").path("coordinates");
        double distanceMeters = summary.path("distance").asDouble(0);
        double durationSeconds = summary.path("duration").asDouble(0);
        if (distanceMeters <= 0 || durationSeconds < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Itineraire invalide retourne par le service");
        }

        double km = Math.round((distanceMeters / 1000.0) * 1000.0) / 1000.0;
        long min = Math.max(1, Math.round(durationSeconds / 60.0));
        List<RoutePoint> coordinates = new ArrayList<>();
        if (coordinatesNode.isArray()) {
            for (JsonNode pointNode : coordinatesNode) {
                if (pointNode.isArray() && pointNode.size() >= 2) {
                    coordinates.add(new RoutePoint(
                            pointNode.path(1).asDouble(),
                            pointNode.path(0).asDouble()));
                }
            }
        }
        return new RouteResult(km, min, coordinates);
    }

    private void validateCoordinates(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (!isLatitude(lat1) || !isLatitude(lat2) || !isLongitude(lon1) || !isLongitude(lon2)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordonnees invalides");
        }
    }

    private boolean isLatitude(Double value) {
        return value != null && value >= -90 && value <= 90;
    }

    private boolean isLongitude(Double value) {
        return value != null && value >= -180 && value <= 180;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record RoutePoint(double lat, double lng) {}

    public record RouteResult(double km, long min, List<RoutePoint> coordinates) {}
}
