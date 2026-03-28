package com.transport.transport.config;

import java.io.IOException;
import java.util.Objects;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.transport.transport.service.PresenceService;

@Component
public class UpdateLastSeenFilter extends OncePerRequestFilter {

  private final PresenceService presence;

  public UpdateLastSeenFilter(PresenceService presence) {
    this.presence = presence;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain) throws ServletException, IOException {
    try {
      String path = request.getRequestURI();

      if (!shouldNotFilter(request)) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuth = auth != null && auth.isAuthenticated()
            && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()));

        String principal = null;

        if (isAuth) {
          String headerId = request.getHeader("X-User-Id");
          if (headerId != null && !headerId.isBlank()) {
            principal = headerId;
          } else if (auth instanceof JwtAuthenticationToken jat) {
            Object userIdClaim = jat.getToken().getClaims()
                .getOrDefault("userId", jat.getToken().getSubject());
            principal = Objects.toString(userIdClaim, null);
          } else if (auth instanceof UsernamePasswordAuthenticationToken) {
            principal = auth.getName();
          } else {
            principal = auth.getName();
          }
        }

        if (principal != null && !principal.isBlank()) {
          presence.heartbeat(principal);
        }
      }
    } catch (Exception ignore) {
      // Ne bloque jamais la requete en cas d'erreur de presence.
    }

    chain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String p = request.getRequestURI();
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
    return p.startsWith("/auth/") || p.startsWith("/public/") || p.startsWith("/actuator/");
  }
}
