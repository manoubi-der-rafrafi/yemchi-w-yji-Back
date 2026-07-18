package com.transport.transport.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.service.RoutingService;
import com.transport.transport.service.RoutingService.RouteResult;

@RestController
@RequestMapping("/api/routing")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/distance")
    public ResponseEntity<RouteDistanceResponse> getRouteDistance(@RequestBody RouteDistanceRequest request) {
        RouteResult route = calculate(request);
        return ResponseEntity.ok(new RouteDistanceResponse(route.km(), route.min()));
    }

    @PostMapping("/geometry")
    public ResponseEntity<RouteGeometryResponse> getRouteGeometry(@RequestBody RouteDistanceRequest request) {
        RouteResult route = calculate(request);
        List<RoutePoint> coordinates = route.coordinates().stream()
                .map(point -> new RoutePoint(point.lat(), point.lng()))
                .toList();
        return ResponseEntity.ok(new RouteGeometryResponse(route.km(), route.min(), coordinates));
    }

    private RouteResult calculate(RouteDistanceRequest request) {
        if (request == null) {
            return routingService.calculateRoute(null, null, null, null);
        }
        return routingService.calculateRoute(request.lat1(), request.lon1(), request.lat2(), request.lon2());
    }

    public record RouteDistanceRequest(Double lat1, Double lon1, Double lat2, Double lon2) {}

    public record RouteDistanceResponse(double km, long min) {}

    public record RoutePoint(double lat, double lng) {}

    public record RouteGeometryResponse(double km, long min, List<RoutePoint> coordinates) {}
}
