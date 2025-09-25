// com.transport.transport.config.SecurityConfig
package com.transport.transport.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
import com.transport.transport.repository.UtilisateurRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Value("${app.jwt.secret}")
  private String secret;

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // hash sécurisé
  }

  /** On authentifie par email (username = email) */
  @Bean
  UserDetailsService userDetailsService(UtilisateurRepository repo) {
    return (String username) -> repo.findByEmailIgnoreCase(username)
      .map(u -> User.withUsername(u.getEmail())
          .password(u.getMotDePasse()) // doit être {bcrypt}<hash>
          .roles(u.getRole().name().toUpperCase())
          .disabled(u.getStatut() != com.transport.transport.model.Utilisateur.Statut.actif)
          .build())
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

  // ===== JWT signer/decoder (HS256) =====
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

  // ===== CORS pour Angular local =====
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("http://localhost:4200"));
    cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization","Content-Type","X-Requested-With"));
    cfg.setAllowCredentials(true);
    cfg.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  @Bean
SecurityFilterChain security(HttpSecurity http, UpdateLastSeenFilter lastSeenFilter) throws Exception {
  http
    .csrf(csrf -> csrf.disable())
    .cors(Customizer.withDefaults())
    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/utilisateur/login").permitAll() // ✅
        .requestMatchers("/auth/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/presence/heartbeat").permitAll()
        .requestMatchers(HttpMethod.GET,  "/api/presence/**").permitAll()
        .requestMatchers("/api/produits/**").permitAll()
        .requestMatchers("/api/utilisateur/register").permitAll()
        .requestMatchers("/api/commandes/**").authenticated()
        .anyRequest().authenticated()
    )


    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

  http.addFilterAfter(
    lastSeenFilter,
    org.springframework.security.web.context.SecurityContextHolderFilter.class
  );

  return http.build();
}
}
