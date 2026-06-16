package com.transport.transport.security;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.transport.transport.service.PartnerApiKeyService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PartnerAuthenticationFilter extends OncePerRequestFilter {

    private final PartnerApiKeyService partnerApiKeyService;

    public PartnerAuthenticationFilter(PartnerApiKeyService partnerApiKeyService) {
        this.partnerApiKeyService = partnerApiKeyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/partner/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String apiKey = resolveApiKey(request);
        if (apiKey == null || apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        PartnerPrincipal principal = partnerApiKeyService.authenticate(apiKey);
        var authorities = principal.getScopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toSet());
        authorities.add(new SimpleGrantedAuthority("ROLE_PARTNER"));

        var authentication = new UsernamePasswordAuthenticationToken(
                principal,
                apiKey,
                authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String resolveApiKey(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("ApiKey ")) {
            return authorization.substring("ApiKey ".length()).trim();
        }
        String apiKey = request.getHeader("X-API-Key");
        return apiKey != null ? apiKey.trim() : null;
    }
}
