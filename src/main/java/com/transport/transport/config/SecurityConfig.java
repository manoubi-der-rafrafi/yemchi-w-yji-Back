// com.transport.transport.config.SecurityConfig
package com.transport.transport.config;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  GoogleIdTokenVerifier googleVerifier(@Value("${app.google.client-id}") String clientId) {
    return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
        .setAudience(List.of(clientId))
        .build();
  }

  @Value("${app.jwt.secret}")
  private String secret;

  /* =========================
     PasswordEncoder
     - Delegating : supporte {bcrypt}... ou hash $2a$... sans préfixe
     ========================= */
  @Bean
PasswordEncoder passwordEncoder() {
  String id = "bcrypt";
  Map<String, PasswordEncoder> encoders = new HashMap<>();
  encoders.put(id, new BCryptPasswordEncoder());
  DelegatingPasswordEncoder d = new DelegatingPasswordEncoder(id, encoders);
  // ⬇️ Permettre les hashes SANS préfixe (ex: $2a$...) lors des matches()
  d.setDefaultPasswordEncoderForMatches(encoders.get(id));
  return d;
}


  /* =========================
     UserDetailsService
     - username = email
     - disabled si statut != actif
     - rôle par défaut "CLIENT" si null (sécurité)
     ========================= */
  @Bean
UserDetailsService userDetailsService(UtilisateurRepository repo) {
  return (String username) -> repo.findByEmailIgnoreCase(username)
    .map(u -> {
      String role = (u.getRole() != null) ? u.getRole().name() : "CLIENT";
      role = role.toUpperCase().replaceFirst("^ROLE_", ""); // .roles() ajoute ROLE_

      // Ne désactive que les comptes bannis. "inactif" = présence, pas un blocage d'auth.
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

  /* =========================
     JWT signer/decoder (HS256)
     ========================= */
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

  /* =========================
     CORS (UNIQUE SOURCE DE VÉRITÉ)
     - Autorise localhost:4200, 127.0.0.1:4200 et ton front Vercel
     ========================= */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();

    // Autorise Flutter Web en dev (ports variables) + ton domaine de prod
    cfg.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "http://127.0.0.1:*",
        "https://yemchi-w-yji-front.vercel.app",
        "http://10.0.2.2:5173"
    ));

    cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With","Origin"));
    cfg.setExposedHeaders(List.of("Authorization","Location"));

    // Avec JWT en header, pas besoin de cookies → false (plus simple)
    // Mets true seulement si tu utilises des cookies/sessions côté navigateur.
    cfg.setAllowCredentials(false);

    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
}

  /* =========================
     Security Filter Chain
     ========================= */
  @Bean
  SecurityFilterChain security(HttpSecurity http, UpdateLastSeenFilter lastSeenFilter) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
          .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
          .requestMatchers(HttpMethod.POST, "/api/utilisateur/login").permitAll()
          .requestMatchers(HttpMethod.POST, "/api/utilisateur/login/google").permitAll()
          .requestMatchers("/auth/**").permitAll()
          .requestMatchers(HttpMethod.POST, "/api/presence/heartbeat").authenticated()
          .requestMatchers(HttpMethod.GET,  "/api/presence/**").authenticated()
          .requestMatchers("/api/produits/**").permitAll()
          .requestMatchers("/api/utilisateur/register").permitAll()
          .requestMatchers("/api/commandes/**").authenticated()
          .requestMatchers("/api/demandes/**").authenticated()
          .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

    // Ne pas perturber l'auth : exécute après que le SecurityContext soit établi
    http.addFilterAfter(
      lastSeenFilter,
      org.springframework.security.web.context.SecurityContextHolderFilter.class
    );

    return http.build();
  }
}
