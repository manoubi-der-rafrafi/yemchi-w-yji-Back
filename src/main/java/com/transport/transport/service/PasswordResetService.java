package com.transport.transport.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.PasswordResetToken;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.PasswordResetTokenRepository;
import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.security.PasswordPolicy;

@Service
public class PasswordResetService {
  private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UtilisateurRepository users;
  private final PasswordResetTokenRepository tokens;
  private final MongoTemplate mongo;
  private final PasswordEncoder passwordEncoder;
  private final MailService mailService;
  private final AuthTokenService authTokenService;
  private final String resetBaseUrl;
  private final long lifetimeSeconds;

  public PasswordResetService(
      UtilisateurRepository users,
      PasswordResetTokenRepository tokens,
      MongoTemplate mongo,
      PasswordEncoder passwordEncoder,
      MailService mailService,
      AuthTokenService authTokenService,
      @Value("${app.password-reset.base-url:yemchiwyji://reset-password}") String resetBaseUrl,
      @Value("${app.password-reset.token-seconds:900}") long lifetimeSeconds) {
    this.users = users;
    this.tokens = tokens;
    this.mongo = mongo;
    this.passwordEncoder = passwordEncoder;
    this.mailService = mailService;
    this.authTokenService = authTokenService;
    this.resetBaseUrl = stripTrailingQuestionMark(resetBaseUrl);
    this.lifetimeSeconds = Math.max(300, Math.min(lifetimeSeconds, 3600));
  }

  /**
   * Demande un lien sans reveler si l'adresse existe. Les erreurs d'envoi restent
   * dans les journaux serveur et ne changent jamais la reponse publique.
   */
  public void requestReset(String email) {
    String normalized = normalizeAndValidateEmail(email);
    Utilisateur user = users.findByEmailIgnoreCase(normalized).orElse(null);
    if (!isEligible(user)) {
      performDummyHash(normalized);
      return;
    }

    Instant now = Instant.now();
    invalidateOutstandingTokens(user.getId(), now);
    String rawToken = randomToken();
    PasswordResetToken token = new PasswordResetToken();
    token.setTokenHash(hash(rawToken));
    token.setUserId(user.getId());
    token.setCreatedAt(now);
    token.setExpiresAt(now.plusSeconds(lifetimeSeconds));
    tokens.save(token);

    try {
      mailService.sendPasswordResetEmail(
          user.getEmail(), resetBaseUrl + "?token=" + rawToken, lifetimeSeconds / 60);
    } catch (RuntimeException error) {
      invalidateOutstandingTokens(user.getId(), Instant.now());
      logger.error("Password reset email delivery failed userId={}", user.getId(), error);
    }
  }

  /** Consomme atomiquement le jeton puis change le mot de passe. */
  public void resetPassword(String rawToken, String newPassword) {
    if (rawToken == null || rawToken.isBlank() || rawToken.length() > 256) {
      throw invalidToken();
    }
    PasswordPolicy.requireStrong(newPassword);

    Instant now = Instant.now();
    String tokenHash = hash(rawToken);
    PasswordResetToken candidate = tokens.findById(tokenHash).orElseThrow(
        PasswordResetService::invalidToken);
    if (candidate.getConsumedAt() != null || candidate.getExpiresAt() == null
        || !candidate.getExpiresAt().isAfter(now)) {
      throw invalidToken();
    }
    Utilisateur user = users.findById(candidate.getUserId()).orElseThrow(
        PasswordResetService::invalidToken);
    if (user.getMotDePasse() != null && passwordEncoder.matches(newPassword, user.getMotDePasse())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Le nouveau mot de passe doit etre different de l'ancien");
    }

    PasswordResetToken claimed = mongo.findAndModify(
        Query.query(Criteria.where("_id").is(tokenHash)
            .and("consumedAt").is(null)
            .and("expiresAt").gt(now)),
        new Update().set("consumedAt", now),
        PasswordResetToken.class);
    if (claimed == null) {
      throw invalidToken();
    }

    user.setMotDePasse(passwordEncoder.encode(newPassword));
    users.save(user);
    invalidateOutstandingTokens(user.getId(), now);
    authTokenService.revokeAllForUser(user.getId());
  }

  /** Nettoyage de secours lorsque la creation automatique des index TTL est desactivee. */
  @Scheduled(fixedDelayString = "${app.password-reset.cleanup-ms:3600000}")
  public void cleanupExpiredTokens() {
    mongo.remove(
        Query.query(Criteria.where("expiresAt").lt(Instant.now())),
        PasswordResetToken.class);
  }

  private void invalidateOutstandingTokens(String userId, Instant now) {
    mongo.updateMulti(
        Query.query(Criteria.where("userId").is(userId).and("consumedAt").is(null)),
        new Update().set("consumedAt", now),
        PasswordResetToken.class);
  }

  private static boolean isEligible(Utilisateur user) {
    return user != null
        && user.getId() != null
        && user.getEmail() != null
        && user.getStatut() != Utilisateur.Statut.banni
        && user.getMotDePasse() != null
        && !user.getMotDePasse().isBlank();
  }

  private static String normalizeAndValidateEmail(String email) {
    String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    int at = normalized.indexOf('@');
    if (normalized.isEmpty() || normalized.length() > 254 || at <= 0
        || at != normalized.lastIndexOf('@') || at == normalized.length() - 1
        || normalized.indexOf('.', at) < at + 2) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresse email invalide");
    }
    return normalized;
  }

  private static void performDummyHash(String value) {
    hash("missing-user:" + value);
  }

  private static String randomToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String hash(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 indisponible", impossible);
    }
  }

  private static String stripTrailingQuestionMark(String value) {
    String result = value == null ? "" : value.trim();
    while (result.endsWith("?") || result.endsWith("&")) {
      result = result.substring(0, result.length() - 1);
    }
    if (result.isBlank()) {
      throw new IllegalArgumentException("app.password-reset.base-url est manquant");
    }
    return result;
  }

  private static ResponseStatusException invalidToken() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Lien de reinitialisation invalide, expire ou deja utilise");
  }
}
