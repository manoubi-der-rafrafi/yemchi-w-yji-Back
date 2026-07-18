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
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service de routage non configure");
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
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service de routage indisponible");
            }
            return parseRoute(response.body());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erreur reseau vers le service de routage");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Appel au service de routage interrompu");
        }
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
