// com.transport.transport.config.UpdateLastSeenFilter
package com.transport.transport.config;

import java.io.IOException;
import java.util.Objects;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.transport.transport.service.PresenceService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

      // On ne s'en mêle pas pour auth/public (login, static, etc.)
      if (!shouldNotFilter(request)) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuth = auth != null && auth.isAuthenticated()
            && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()));

        String principal = null;

        if (isAuth) {
          // 1) Header prioritaire côté DEV seulement si déjà authentifié
          String headerId = request.getHeader("X-User-Id");
          if (headerId != null && !headerId.isBlank()) {
            principal = headerId;
          } else if (auth instanceof JwtAuthenticationToken jat) {
            // 2) JWT → claim "userId" sinon "sub"
            Object userIdClaim = jat.getToken().getClaims()
                .getOrDefault("userId", jat.getToken().getSubject());
            principal = Objects.toString(userIdClaim, null);
          } else if (auth instanceof UsernamePasswordAuthenticationToken) {
            // 3) Basic / in-memory
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
      // ne bloque jamais la requête en cas d'erreur de présence
    }

    chain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String p = request.getRequestURI();
    // Ignore les préflights CORS + endpoints publics
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
    return p.startsWith("/auth/") || p.startsWith("/public/") || p.startsWith("/actuator/");
  }
}
