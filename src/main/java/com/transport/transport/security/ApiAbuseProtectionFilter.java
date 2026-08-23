package com.transport.transport.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.transport.transport.service.RequestRateLimiter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiAbuseProtectionFilter extends OncePerRequestFilter {
  private static final long TELEMETRY_MAX_BYTES = 64 * 1024L;

  private final RequestRateLimiter limiter;
  private final boolean trustProxyHeaders;

  public ApiAbuseProtectionFilter(RequestRateLimiter limiter, boolean trustProxyHeaders) {
    this.limiter = limiter;
    this.trustProxyHeaders = trustProxyHeaders;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    Rule rule = ruleFor(request.getMethod(), request.getRequestURI(), request.getContentType());
    if (rule == null) {
      chain.doFilter(request, response);
      return;
    }

    if (rule.telemetry() && request.getContentLengthLong() > TELEMETRY_MAX_BYTES) {
      writeError(response, HttpStatus.PAYLOAD_TOO_LARGE.value(), "Requete trop volumineuse", 0);
      return;
    }

    String ip = clientIp(request);
    RequestRateLimiter.Result ipResult = limiter.acquire(
        rule.scope() + ":ip", ip, rule.ipLimit(), rule.windowSeconds());
    if (!ipResult.allowed()) {
      writeError(response, HttpStatus.TOO_MANY_REQUESTS.value(),
          "Trop de requetes. Reessayez plus tard.", ipResult.retryAfterSeconds());
      return;
    }

    String principal = principal();
    if (principal != null) {
      RequestRateLimiter.Result userResult = limiter.acquire(
          rule.scope() + ":user", principal, rule.userLimit(), rule.windowSeconds());
      if (!userResult.allowed()) {
        writeError(response, HttpStatus.TOO_MANY_REQUESTS.value(),
            "Trop de requetes. Reessayez plus tard.", userResult.retryAfterSeconds());
        return;
      }
    }

    chain.doFilter(request, response);
  }

  private Rule ruleFor(String method, String uri, String contentType) {
    if ("POST".equals(method) && ("/api/utilisateur/login".equals(uri)
        || "/api/utilisateur/login/google".equals(uri))) {
      return new Rule("login", 20, 10, 300, false);
    }
    if ("POST".equals(method) && uri.startsWith("/api/utilisateur/register")) {
      return new Rule("register", 12, 8, 3600, false);
    }
    if ("GET".equals(method) && ("/api/utilisateur/verify-email".equals(uri)
        || "/api/utilisateur/email-verification-status".equals(uri))) {
      return new Rule("email-verification", 60, 30, 3600, false);
    }
    if ("GET".equals(method) && ("/api/utilisateur/search/email".equals(uri)
        || "/api/utilisateur/search/numero".equals(uri))) {
      return new Rule("user-search", 180, 60, 60, false);
    }
    if ("GET".equals(method) && uri.startsWith("/api/presence/")) {
      return new Rule("presence-status", 180, 60, 60, false);
    }
    if ("POST".equals(method) && ("/api/analytics/events".equals(uri)
        || "/api/errors".equals(uri))) {
      return new Rule("telemetry", 120, 120, 60, true);
    }
    if ("POST".equals(method) && uri.startsWith("/api/routing/")) {
      return new Rule("routing", 120, 30, 60, false);
    }
    if ("POST".equals(method) && uri.startsWith("/api/auth/")) {
      return new Rule("auth-session", 60, 40, 60, false);
    }
    if (("POST".equals(method) || "PATCH".equals(method))
        && uri.startsWith("/api/internal/partners/")) {
      return new Rule("partner-provision", 10, 10, 300, false);
    }
    if ("POST".equals(method) && "/api/utilisateur/send-email".equals(uri)) {
      return new Rule("send-email", 20, 10, 3600, false);
    }
    if ("POST".equals(method) && (uri.contains("/upload") || uri.endsWith("/detect")
        || (contentType != null && contentType.toLowerCase().startsWith("multipart/")))) {
      return new Rule("media", 60, 20, 600, false);
    }
    return null;
  }

  private String principal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
      return null;
    }
    return authentication.getName();
  }

  private String clientIp(HttpServletRequest request) {
    if (trustProxyHeaders) {
      String realIp = request.getHeader("X-Real-IP");
      if (realIp != null && !realIp.isBlank()) {
        return realIp.trim();
      }
    }
    return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
  }

  private void writeError(HttpServletResponse response, int status, String message, long retryAfter)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    if (retryAfter > 0) {
      response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
    }
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }

  private record Rule(
      String scope,
      int ipLimit,
      int userLimit,
      long windowSeconds,
      boolean telemetry) {}
}
