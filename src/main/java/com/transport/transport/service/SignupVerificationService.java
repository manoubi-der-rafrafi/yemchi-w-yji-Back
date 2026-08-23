package com.transport.transport.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.SignupVerificationSession;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.SignupVerificationSessionRepository;

@Service
public class SignupVerificationService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final long LIFETIME_SECONDS = 30 * 60L;

  private final SignupVerificationSessionRepository sessions;
  private final MongoTemplate mongo;

  public SignupVerificationService(SignupVerificationSessionRepository sessions, MongoTemplate mongo) {
    this.sessions = sessions;
    this.mongo = mongo;
  }

  public StartedSession start(Utilisateur user) {
    Instant now = Instant.now();
    mongo.updateMulti(
        Query.query(Criteria.where("userId").is(user.getId()).and("consumedAt").is(null)),
        new Update().set("consumedAt", now), SignupVerificationSession.class);

    String clientToken = randomToken();
    SignupVerificationSession session = new SignupVerificationSession();
    session.setId(UUID.randomUUID().toString());
    session.setUserId(user.getId());
    session.setEmail(normalizeEmail(user.getEmail()));
    session.setClientTokenHash(hash(clientToken));
    session.setCreatedAt(now);
    session.setExpiresAt(now.plusSeconds(LIFETIME_SECONDS));
    sessions.save(session);
    return new StartedSession(session.getId(), clientToken);
  }

  public void markVerified(String sessionId, String userId) {
    Instant now = Instant.now();
    var result = mongo.updateFirst(
        Query.query(Criteria.where("_id").is(sessionId)
            .and("userId").is(userId)
            .and("consumedAt").is(null)
            .and("expiresAt").gt(now)),
        new Update().set("verifiedAt", now), SignupVerificationSession.class);
    if (result.getModifiedCount() != 1 && result.getMatchedCount() != 1) {
      throw invalid();
    }
  }

  public boolean isVerified(String email, String clientToken) {
    SignupVerificationSession session = findValid(email, clientToken);
    return session.getVerifiedAt() != null;
  }

  public SignupVerificationSession consumeVerified(String email, String clientToken) {
    Instant now = Instant.now();
    String normalizedEmail = normalizeEmail(email);
    if (clientToken == null || clientToken.isBlank()) throw invalid();
    SignupVerificationSession claimed = mongo.findAndModify(
        Query.query(Criteria.where("clientTokenHash").is(hash(clientToken))
            .and("email").is(normalizedEmail)
            .and("verifiedAt").ne(null)
            .and("consumedAt").is(null)
            .and("expiresAt").gt(now)),
        new Update().set("consumedAt", now), SignupVerificationSession.class);
    if (claimed == null) throw invalid();
    return claimed;
  }

  private SignupVerificationSession findValid(String email, String clientToken) {
    if (clientToken == null || clientToken.isBlank()) throw invalid();
    SignupVerificationSession session = sessions.findByClientTokenHash(hash(clientToken))
        .orElseThrow(SignupVerificationService::invalid);
    if (!session.getEmail().equals(normalizeEmail(email)) || session.getConsumedAt() != null
        || session.getExpiresAt() == null || !session.getExpiresAt().isAfter(Instant.now())) {
      throw invalid();
    }
    return session;
  }

  private static String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  private static String randomToken() {
    byte[] value = new byte[32];
    RANDOM.nextBytes(value);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  private static String hash(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static ResponseStatusException invalid() {
    return new ResponseStatusException(HttpStatus.FORBIDDEN,
        "Session d'inscription invalide, expiree ou deja utilisee");
  }

  public record StartedSession(String sessionId, String clientToken) {}
}
