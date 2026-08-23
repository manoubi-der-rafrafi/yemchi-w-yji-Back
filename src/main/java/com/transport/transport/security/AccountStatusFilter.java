package com.transport.transport.security;

import java.io.IOException;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Bloque immediatement tout JWT appartenant a un compte banni ou supprime. */
@Component
public class AccountStatusFilter extends OncePerRequestFilter {
  private final UtilisateurRepository users;

  public AccountStatusFilter(UtilisateurRepository users) {
    this.users = users;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwt)) {
      chain.doFilter(request, response);
      return;
    }

    String userId = Objects.toString(jwt.getToken().getClaim("uid"), "");
    Utilisateur user = userId.isBlank() ? null : users.findById(userId).orElse(null);
    if (user == null || user.getStatut() == Utilisateur.Statut.banni) {
      SecurityContextHolder.clearContext();
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write("{\"error\":\"Compte indisponible\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}
