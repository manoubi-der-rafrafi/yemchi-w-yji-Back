// com.transport.transport.config
package com.transport.transport.config;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.security.PartnerAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

  @Value("${app.jwt.secret}")
  private String secret;

  @Bean
  GoogleIdTokenVerifier googleVerifier(@Value("${app.google.client-id}") String clientId) {
    return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
        .setAudience(List.of(clientId))
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    String id = "bcrypt";
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put(id, new BCryptPasswordEncoder());
    DelegatingPasswordEncoder d = new DelegatingPasswordEncoder(id, encoders);
    d.setDefaultPasswordEncoderForMatches(encoders.get(id));
    return d;
  }

  @Bean
  UserDetailsService userDetailsService(UtilisateurRepository repo) {
    return username -> repo.findByEmailIgnoreCase(username)
        .map(u -> {
          String role = (u.getRole() != null) ? u.getRole().name() : "CLIENT";
          role = role.toUpperCase().replaceFirst("^ROLE_", "");
          boolean disabled = (u.getStatut() == Utilisateur.Statut.banni);

          return User.withUsername(u.getEmail())
              .password(u.getMotDePasse())
              .roles(role)
              .accountExpired(false)
              .accountLocked(false)
              .credentialsExpired(false)
              .disabled(disabled)
              .build();
        })
        .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable: " + username));
  }

  @Bean
  DaoAuthenticationProvider daoAuthProvider(UserDetailsService uds, PasswordEncoder pe) {
    DaoAuthenticationProvider p = new DaoAuthenticationProvider();
    p.setUserDetailsService(uds);
    p.setPasswordEncoder(pe);
    return p;
  }

  @Bean
  AuthenticationManager authManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  @Bean
  JwtEncoder jwtEncoder() {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder() {
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
    grantedAuthoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
    authenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return authenticationConverter;
  }

  @Bean
  BearerTokenResolver bearerTokenResolver() {
    DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
    return request -> isPublicEndpoint(request) ? null : delegate.resolve(request);
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "http://127.0.0.1:*",
        "https://yemchi-w-yji-front.vercel.app",
        "https://www.yemchi-w-yji.tn",
        "http://10.0.2.2:5173"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "X-API-Key"));
    cfg.setExposedHeaders(List.of("Authorization", "Location"));
    cfg.setAllowCredentials(false);
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      UpdateLastSeenFilter lastSeenFilter,
      PartnerAuthenticationFilter partnerAuthenticationFilter,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      BearerTokenResolver bearerTokenResolver) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
              logger.warn("401 unauthorized method={} uri={} message={}",
                  request.getMethod(),
                  request.getRequestURI(),
                  authException.getMessage());
              response.sendError(401);
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              var auth = org.springframework.security.core.context.SecurityContextHolder
                  .getContext()
                  .getAuthentication();
              logger.warn("403 forbidden method={} uri={} principal={} authorities={} message={}",
                  request.getMethod(),
                  request.getRequestURI(),
                  auth != null ? auth.getName() : null,
                  auth != null ? auth.getAuthorities() : null,
                  accessDeniedException.getMessage());
              response.sendError(403);
            }))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/utilisateur/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/utilisateur/login/google").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/utilisateur/register/email").permitAll()
            .requestMatchers("/api/utilisateur/register/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/utilisateur/verify-email").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/utilisateur/email-verification-status").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/internal/partners/provision").permitAll()
            .requestMatchers("/auth/**").permitAll()
            .requestMatchers("/api/utilisateur/register").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/presence/heartbeat").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/presence/**").authenticated()
            .requestMatchers("/api/admin/partners/**").authenticated()
            .requestMatchers("/api/partner/**").authenticated()
            .requestMatchers("/api/commandes/**").authenticated()
            .requestMatchers("/api/demandes/**").authenticated()
            .requestMatchers("/api/produits/**").authenticated()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .bearerTokenResolver(bearerTokenResolver)
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

    http.addFilterBefore(partnerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(
        lastSeenFilter,
        org.springframework.security.web.context.SecurityContextHolderFilter.class);

    return http.build();
  }

  private boolean isPublicEndpoint(jakarta.servlet.http.HttpServletRequest request) {
    String method = request.getMethod();
    String uri = request.getRequestURI();

    if (HttpMethod.OPTIONS.matches(method)) {
      return true;
    }
    if (HttpMethod.POST.matches(method)) {
      return "/api/utilisateur/login".equals(uri)
          || "/api/utilisateur/login/google".equals(uri)
          || "/api/utilisateur/register".equals(uri)
          || "/api/utilisateur/register/email".equals(uri)
          || "/api/utilisateur/register/complete".equals(uri)
          || "/error".equals(uri);
    }
    if (HttpMethod.GET.matches(method)) {
      return "/error".equals(uri)
          || "/api/utilisateur/verify-email".equals(uri)
          || "/api/utilisateur/email-verification-status".equals(uri);
    }
    return false;
  }
}
