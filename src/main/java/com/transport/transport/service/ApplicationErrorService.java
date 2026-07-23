package com.transport.transport.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.transport.transport.dto.ApplicationErrorRequest;
import com.transport.transport.model.ApplicationError;
import com.transport.transport.repository.ApplicationErrorRepository;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class ApplicationErrorService {
  private static final Set<String> SOURCES =
      Set.of("web_client", "web_admin", "mobile_client", "mobile_driver", "backend");
  private static final Set<String> SEVERITIES = Set.of("info", "warning", "error", "critical");
  private static final Set<String> SENSITIVE_KEYS = Set.of(
      "password", "motdepasse", "mot_de_passe", "token", "accesstoken", "access_token",
      "refreshtoken", "refresh_token", "authorization", "cookie", "set-cookie", "secret",
      "apikey", "api_key", "image", "base64", "latitude", "longitude", "coordinates");
  private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[a-z0-9._~+\\-/]+=*");
  private static final Pattern JWT = Pattern.compile("(?i)\\beyJ[a-z0-9_-]+\\.[a-z0-9_-]+\\.[a-z0-9_-]+\\b");
  private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
      "(?i)(password|motDePasse|token|accessToken|refreshToken|authorization|secret)\\s*[:=]\\s*[^\\s,;]+");

  private final ApplicationErrorRepository errors;
  private final UtilisateurRepository users;

  public ApplicationErrorService(ApplicationErrorRepository errors, UtilisateurRepository users) {
    this.errors = errors;
    this.users = users;
  }

  public void collect(ApplicationErrorRequest request, String authenticatedEmail, String userAgent) {
    validate(request);

    ApplicationError error = new ApplicationError();
    error.setMessage(sanitizeText(request.message(), 1200));
    error.setType(sanitizeText(defaultValue(request.type(), "unknown"), 100));
    error.setSource(request.source());
    error.setSeverity(normalizeSeverity(request.severity()));
    error.setPage(sanitizeEndpoint(request.page()));
    error.setEndpoint(sanitizeEndpoint(request.endpoint()));
    error.setHttpStatus(validStatus(request.httpStatus()));
    error.setStackTrace(sanitizeText(request.stackTrace(), 6000));
    error.setMetadata(sanitizeMetadata(request.metadata()));
    error.setAppVersion(sanitizeText(request.appVersion(), 50));
    error.setDeviceType(sanitizeText(request.deviceType(), 80));
    error.setUserAgent(sanitizeText(userAgent, 400));
    error.setCreatedAt(Instant.now());

    if (authenticatedEmail != null) {
      users.findByEmailIgnoreCase(authenticatedEmail).ifPresent(user -> {
        error.setUserId(user.getId());
        error.setUserEmail(sanitizeText(user.getEmail(), 200));
        error.setUserName(sanitizeText(
            String.join(" ", nonBlank(user.getPrenom()), nonBlank(user.getNom())).trim(), 160));
        error.setRole(user.getRole() == null ? null : user.getRole().name());
      });
    }
    errors.save(error);
  }

  public Page<ApplicationError> list(int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(size, 100));
    return errors.findAll(PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
  }

  private void validate(ApplicationErrorRequest request) {
    if (request == null || request.message() == null || request.message().isBlank()) {
      throw new IllegalArgumentException("message est obligatoire");
    }
    if (request.source() == null || !SOURCES.contains(request.source())) {
      throw new IllegalArgumentException("source invalide");
    }
    if (request.metadata() != null && request.metadata().size() > 20) {
      throw new IllegalArgumentException("metadata contient trop de proprietes");
    }
  }

  private String normalizeSeverity(String value) {
    String normalized = value == null ? "error" : value.toLowerCase(Locale.ROOT);
    return SEVERITIES.contains(normalized) ? normalized : "error";
  }

  private Integer validStatus(Integer status) {
    return status != null && status >= 100 && status <= 599 ? status : null;
  }

  private String sanitizeEndpoint(String endpoint) {
    if (endpoint == null) return null;
    int query = endpoint.indexOf('?');
    return sanitizeText(query >= 0 ? endpoint.substring(0, query) : endpoint, 300);
  }

  private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) return Map.of();
    Map<String, Object> result = new LinkedHashMap<>();
    int count = 0;
    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
      if (count >= 20) break;
      String key = entry.getKey();
      if (key == null || key.isBlank() || isSensitiveKey(key)) continue;
      Object value = sanitizeValue(entry.getValue(), 0);
      if (value != null) {
        result.put(sanitizeText(key, 80), value);
        count++;
      }
    }
    return result;
  }

  private Object sanitizeValue(Object value, int depth) {
    if (value == null || depth > 2) return null;
    if (value instanceof String string) return sanitizeText(string, 300);
    if (value instanceof Number || value instanceof Boolean) return value;
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> nested = new LinkedHashMap<>();
      int count = 0;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (count >= 10) break;
        String key = String.valueOf(entry.getKey());
        if (isSensitiveKey(key)) continue;
        Object clean = sanitizeValue(entry.getValue(), depth + 1);
        if (clean != null) {
          nested.put(sanitizeText(key, 80), clean);
          count++;
        }
      }
      return nested;
    }
    if (value instanceof Iterable<?> iterable) {
      List<Object> items = new ArrayList<>();
      for (Object item : iterable) {
        if (items.size() >= 10) break;
        Object clean = sanitizeValue(item, depth + 1);
        if (clean != null) items.add(clean);
      }
      return items;
    }
    return sanitizeText(String.valueOf(value), 300);
  }

  private boolean isSensitiveKey(String key) {
    String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "");
    return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
  }

  private String sanitizeText(String value, int maxLength) {
    if (value == null || value.isBlank()) return null;
    String clean = BEARER.matcher(value).replaceAll("Bearer [REDACTED]");
    clean = JWT.matcher(clean).replaceAll("[REDACTED_TOKEN]");
    clean = SECRET_ASSIGNMENT.matcher(clean).replaceAll("$1=[REDACTED]");
    clean = clean.trim();
    return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
  }

  private String defaultValue(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String nonBlank(String value) {
    return value == null ? "" : value;
  }
}
