package com.transport.transport.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.transport.transport.service.AuthTokenService;
import com.transport.transport.service.AuthTokenService.TokenPair;

@RestController
@RequestMapping("/api/auth")
public class AuthSessionController {
  public static final String REFRESH_COOKIE = "yw_refresh";

  private final AuthTokenService tokens;
  private final boolean secureCookie;
  private final String sameSite;
  private final Set<String> allowedOrigins;

  public AuthSessionController(
      AuthTokenService tokens,
      @Value("${app.auth.refresh-cookie-secure:true}") boolean secureCookie,
      @Value("${app.auth.refresh-cookie-same-site:Lax}") String sameSite,
      @Value("${app.auth.allowed-origins:https://www.yemchi-w-yji.tn,https://yemchi-w-yji-front.vercel.app,http://localhost:4200}") String allowedOrigins) {
    this.tokens = tokens;
    this.secureCookie = secureCookie;
    this.sameSite = sameSite;
    this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(
      @RequestBody(required = false) RefreshRequest request,
      @CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken,
      HttpServletRequest servletRequest) {
    boolean nativeClient = request != null && request.refreshToken() != null
        && !request.refreshToken().isBlank();
    String submittedToken = nativeClient ? request.refreshToken() : cookieToken;
    if (!nativeClient) {
      requireAllowedBrowserOrigin(servletRequest);
    }
    try {
      TokenPair pair = tokens.rotate(submittedToken);
      return tokenResponse(pair, nativeClient, HttpStatus.OK);
    } catch (ResponseStatusException exception) {
      return ResponseEntity.status(exception.getStatusCode())
          .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
          .body(Map.of("error", exception.getReason(), "errorCode", "REFRESH_TOKEN_INVALID"));
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @RequestBody(required = false) RefreshRequest request,
      @CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken,
      HttpServletRequest servletRequest) {
    boolean nativeClient = request != null && request.refreshToken() != null
        && !request.refreshToken().isBlank();
    if (!nativeClient) {
      requireAllowedBrowserOrigin(servletRequest);
    }
    String submittedToken = nativeClient ? request.refreshToken() : cookieToken;
    tokens.revoke(submittedToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
        .build();
  }

  public ResponseEntity<LoginTokenResponse> loginResponse(
      TokenPair pair, Object user, boolean nativeClient, HttpStatus status) {
    LoginTokenResponse body = new LoginTokenResponse(
        pair.accessToken(), nativeClient ? pair.refreshToken() : null,
        pair.expiresIn(), user);
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
    if (!nativeClient) {
      builder.header(HttpHeaders.SET_COOKIE, refreshCookie(pair).toString());
    }
    return builder.body(body);
  }

  private ResponseEntity<TokenResponse> tokenResponse(
      TokenPair pair, boolean nativeClient, HttpStatus status) {
    TokenResponse body = new TokenResponse(
        pair.accessToken(), nativeClient ? pair.refreshToken() : null, pair.expiresIn());
    ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
    if (!nativeClient) {
      builder.header(HttpHeaders.SET_COOKIE, refreshCookie(pair).toString());
    }
    return builder.body(body);
  }

  private ResponseCookie refreshCookie(TokenPair pair) {
    long maxAge = Math.max(0, Duration.between(Instant.now(), pair.refreshExpiresAt()).toSeconds());
    return ResponseCookie.from(REFRESH_COOKIE, pair.refreshToken())
        .httpOnly(true)
        .secure(secureCookie)
        .sameSite(sameSite)
        .path("/api/auth")
        .maxAge(maxAge)
        .build();
  }

  private ResponseCookie clearRefreshCookie() {
    return ResponseCookie.from(REFRESH_COOKIE, "")
        .httpOnly(true)
        .secure(secureCookie)
        .sameSite(sameSite)
        .path("/api/auth")
        .maxAge(Duration.ZERO)
        .build();
  }

  private void requireAllowedBrowserOrigin(HttpServletRequest request) {
    String origin = request == null ? null : request.getHeader(HttpHeaders.ORIGIN);
    if (origin == null || !allowedOrigins.contains(origin)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origine non autorisee");
    }
  }

  public record RefreshRequest(String refreshToken) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record TokenResponse(String token, String refreshToken, long expiresIn) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record LoginTokenResponse(String token, String refreshToken, long expiresIn, Object user) {}
}
