package com.transport.transport.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.transport.transport.dto.AnalyticsEventRequest;
import com.transport.transport.model.AnalyticsEvent;
import com.transport.transport.repository.AnalyticsEventRepository;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class AnalyticsService {
  private static final Pattern SAFE_NAME = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
  private static final Set<String> PLATFORMS = Set.of("web_client", "web_admin", "mobile_client", "mobile_driver");

  private final AnalyticsEventRepository events;
  private final UtilisateurRepository users;

  public AnalyticsService(AnalyticsEventRepository events, UtilisateurRepository users) {
    this.events = events;
    this.users = users;
  }

  public void collect(AnalyticsEventRequest request, String authenticatedEmail, String userAgent) {
    validate(request);

    AnalyticsEvent event = new AnalyticsEvent();
    event.setVisitorId(trim(request.visitorId(), 80));
    event.setSessionId(trim(request.sessionId(), 80));
    event.setInstallationId(trim(request.installationId(), 80));
    event.setEventName(request.eventName());
    event.setPlatform(request.platform());
    event.setPage(trim(request.page(), 200));
    event.setMetadata(sanitizeMetadata(request.metadata()));
    event.setDeviceType(trim(request.deviceType(), 40));
    event.setAppVersion(trim(request.appVersion(), 40));
    event.setUserAgent(trim(userAgent, 300));
    event.setCreatedAt(Instant.now());

    if (authenticatedEmail != null) {
      users.findByEmailIgnoreCase(authenticatedEmail).ifPresent(user -> event.setUserId(user.getId()));
    }
    events.save(event);
  }

  public Map<String, Object> overview(Instant from, Instant to) {
    Instant effectiveTo = to == null ? Instant.now() : to;
    Instant effectiveFrom = from == null ? effectiveTo.minus(Duration.ofDays(30)) : from;
    if (!effectiveFrom.isBefore(effectiveTo)) {
      throw new IllegalArgumentException("La date de debut doit preceder la date de fin");
    }

    List<AnalyticsEvent> periodEvents = events.findByCreatedAtBetweenOrderByCreatedAtDesc(effectiveFrom, effectiveTo);
    Map<String, Long> byEvent = periodEvents.stream().collect(Collectors.groupingBy(
        AnalyticsEvent::getEventName, LinkedHashMap::new, Collectors.counting()));
    Map<String, Long> byPlatform = periodEvents.stream().collect(Collectors.groupingBy(
        AnalyticsEvent::getPlatform, LinkedHashMap::new, Collectors.counting()));
    long visitors = periodEvents.stream()
        .map(event -> firstNonBlank(event.getVisitorId(), event.getInstallationId(), event.getUserId()))
        .filter(value -> value != null)
        .distinct()
        .count();
    long sessions = periodEvents.stream().map(AnalyticsEvent::getSessionId)
        .filter(value -> value != null && !value.isBlank()).distinct().count();
    long orders = byEvent.getOrDefault("place_order", 0L);
    long checkoutStarts = byEvent.getOrDefault("start_checkout", 0L);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("from", effectiveFrom);
    result.put("to", effectiveTo);
    result.put("totalEvents", periodEvents.size());
    result.put("uniqueVisitors", visitors);
    result.put("sessions", sessions);
    result.put("orders", orders);
    result.put("conversionRate", visitors == 0 ? 0D : orders * 100D / visitors);
    result.put("checkoutAbandonmentRate", checkoutStarts == 0 ? 0D : Math.max(0D, (checkoutStarts - orders) * 100D / checkoutStarts));
    result.put("eventsByName", byEvent);
    result.put("eventsByPlatform", byPlatform);
    return result;
  }

  private void validate(AnalyticsEventRequest request) {
    if (request == null || request.eventName() == null || !SAFE_NAME.matcher(request.eventName()).matches()) {
      throw new IllegalArgumentException("eventName invalide");
    }
    if (request.platform() == null || !PLATFORMS.contains(request.platform())) {
      throw new IllegalArgumentException("platform invalide");
    }
    if (isBlank(request.visitorId()) && isBlank(request.installationId())) {
      throw new IllegalArgumentException("visitorId ou installationId est obligatoire");
    }
    if (request.metadata() != null && request.metadata().size() > 20) {
      throw new IllegalArgumentException("metadata contient trop de proprietes");
    }
  }

  private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) return Map.of();
    Map<String, Object> sanitized = new LinkedHashMap<>();
    metadata.forEach((key, value) -> {
      if (key != null && SAFE_NAME.matcher(key).matches() && value != null) {
        sanitized.put(key, value instanceof String ? trim((String) value, 200) : value);
      }
    });
    return sanitized;
  }

  private String trim(String value, int maxLength) {
    if (value == null || value.isBlank()) return null;
    String clean = value.trim();
    return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
  }

  private boolean isBlank(String value) { return value == null || value.isBlank(); }

  private String firstNonBlank(String... values) {
    for (String value : values) if (!isBlank(value)) return value;
    return null;
  }
}
