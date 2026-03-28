package com.transport.transport.config;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class PublicEndpointMatcher {

  private final RequestMatcher matcher = new OrRequestMatcher(List.of(
      new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name()),
      new AntPathRequestMatcher("/auth/**"),
      new AntPathRequestMatcher("/error"),
      new AntPathRequestMatcher("/public/**"),
      new AntPathRequestMatcher("/actuator/**"),
      new AntPathRequestMatcher("/api/utilisateur/login", HttpMethod.POST.name()),
      new AntPathRequestMatcher("/api/utilisateur/login/google", HttpMethod.POST.name()),
      new AntPathRequestMatcher("/api/utilisateur/register", HttpMethod.POST.name()),
      new AntPathRequestMatcher("/api/utilisateur/register/**", HttpMethod.POST.name()),
      new AntPathRequestMatcher("/api/utilisateur/send-email", HttpMethod.POST.name()),
      new AntPathRequestMatcher("/api/utilisateur/verify-email", HttpMethod.GET.name()),
      new AntPathRequestMatcher("/api/utilisateur/email-verification-status", HttpMethod.GET.name()),
      new AntPathRequestMatcher("/api/produits/**")));

  public boolean matches(HttpServletRequest request) {
    return matcher.matches(request);
  }

  public RequestMatcher requestMatcher() {
    return matcher;
  }
}
