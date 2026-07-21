package com.transport.transport.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.dto.AnalyticsEventRequest;
import com.transport.transport.service.AnalyticsService;

@RestController
@RequestMapping("/api")
public class AnalyticsController {
  private final AnalyticsService analytics;

  public AnalyticsController(AnalyticsService analytics) {
    this.analytics = analytics;
  }

  @PostMapping("/analytics/events")
  public ResponseEntity<Void> collect(
      @RequestBody AnalyticsEventRequest request,
      Authentication authentication,
      @RequestHeader(value = "User-Agent", required = false) String userAgent) {
    String principal = authentication != null && authentication.isAuthenticated()
        && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()))
            ? authentication.getName()
            : null;
    analytics.collect(request, principal, userAgent);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/admin/analytics/overview")
  @PreAuthorize("hasAnyAuthority('admin', 'ROLE_admin', 'ADMIN', 'ROLE_ADMIN')")
  public Map<String, Object> overview(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    return analytics.overview(from, to);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
  }
}
