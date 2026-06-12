package com.transport.transport.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {

  private static final URI ORS_DIRECTIONS_URI =
      URI.create("https://api.openrouteservice.org/v2/directions/driving-car");

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String orsApiKey;

  public RoutingController(ObjectMapper objectMapper, @Value("${ors.api.key:}") String orsApiKey) {
    this.objectMapper = objectMapper;
    this.orsApiKey = orsApiKey;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  @PostMapping("/distance")
  public ResponseEntity<RouteDistanceResponse> getRouteDistance(@RequestBody RouteDistanceRequest request) {
    if (orsApiKey == null || orsApiKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service de routage non configure");
    }
    validateCoordinates(request);

    URI uri = URI.create(ORS_DIRECTIONS_URI + "?start="
        + encode(request.lon1() + "," + request.lat1())
        + "&end="
        + encode(request.lon2() + "," + request.lat2()));

    HttpRequest orsRequest = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(30))
        .header("Authorization", orsApiKey)
        .header("Accept", "application/json")
        .GET()
        .build();

    try {
      HttpResponse<String> response = httpClient.send(orsRequest, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Service de routage indisponible");
      }
      return ResponseEntity.ok(parseRouteDistance(response.body()));
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erreur reseau vers le service de routage");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Appel au service de routage interrompu");
    }
  }

  private RouteDistanceResponse parseRouteDistance(String body) throws IOException {
    JsonNode root = objectMapper.readTree(body);
    JsonNode summary = root.path("features").path(0).path("properties").path("summary");
    double distanceMeters = summary.path("distance").asDouble(0);
    double durationSeconds = summary.path("duration").asDouble(0);
    double km = Math.round((distanceMeters / 1000.0) * 1000.0) / 1000.0;
    long min = Math.round(durationSeconds / 60.0);
    return new RouteDistanceResponse(km, min);
  }

  private void validateCoordinates(RouteDistanceRequest request) {
    if (request == null
        || !isLatitude(request.lat1())
        || !isLatitude(request.lat2())
        || !isLongitude(request.lon1())
        || !isLongitude(request.lon2())) {
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

  public record RouteDistanceRequest(Double lat1, Double lon1, Double lat2, Double lon2) {}

  public record RouteDistanceResponse(double km, long min) {}
}
