package com.transport.transport.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.RefreshTokenFamily;
import com.transport.transport.model.RefreshTokenSession;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.RefreshTokenFamilyRepository;
import com.transport.transport.repository.RefreshTokenSessionRepository;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class AuthTokenService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final JwtEncoder jwtEncoder;
  private final UtilisateurRepository users;
  private final RefreshTokenFamilyRepository families;
  private final RefreshTokenSessionRepository sessions;
  private final MongoTemplate mongoTemplate;
  private final long accessTokenSeconds;
  private final long refreshTokenSeconds;
  private final long refreshFamilySeconds;

  public AuthTokenService(
      JwtEncoder jwtEncoder,
      UtilisateurRepository users,
      RefreshTokenFamilyRepository families,
      RefreshTokenSessionRepository sessions,
      MongoTemplate mongoTemplate,
      @Value("${app.auth.access-token-seconds:900}") long accessTokenSeconds,
      @Value("${app.auth.refresh-token-seconds:2592000}") long refreshTokenSeconds,
      @Value("${app.auth.refresh-family-seconds:7776000}") long refreshFamilySeconds) {
    this.jwtEncoder = jwtEncoder;
    this.users = users;
    this.families = families;
    this.sessions = sessions;
    this.mongoTemplate = mongoTemplate;
    this.accessTokenSeconds = accessTokenSeconds;
    this.refreshTokenSeconds = refreshTokenSeconds;
    this.refreshFamilySeconds = refreshFamilySeconds;
  }

  public TokenPair createSession(Utilisateur user) {
    Instant now = Instant.now();
    RefreshTokenFamily family = new RefreshTokenFamily();
    family.setId(UUID.randomUUID().toString());
    family.setUserId(user.getId());
    family.setCreatedAt(now);
    family.setExpiresAt(now.plusSeconds(refreshFamilySeconds));
    families.save(family);
    return issuePair(user, family, now);
  }

  public TokenPair rotate(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw unauthorized();
    }

    Instant now = Instant.now();
    String submittedHash = hash(rawRefreshToken);
    Query claimQuery = Query.query(Criteria.where("tokenHash").is(submittedHash)
        .and("revokedAt").is(null)
        .and("expiresAt").gt(now));
    Update claimUpdate = new Update().set("revokedAt", now).set("lastUsedAt", now);
    RefreshTokenSession claimed = mongoTemplate.findAndModify(
        claimQuery, claimUpdate, RefreshTokenSession.class);

    if (claimed == null) {
      sessions.findByTokenHash(submittedHash).ifPresent(session -> revokeFamily(session.getFamilyId(), now));
      throw unauthorized();
    }

    RefreshTokenFamily family = families.findById(claimed.getFamilyId()).orElse(null);
    if (family == null || family.getRevokedAt() != null || !family.getExpiresAt().isAfter(now)) {
      revokeFamily(claimed.getFamilyId(), now);
      throw unauthorized();
    }

    Utilisateur user = users.findById(claimed.getUserId()).orElse(null);
    if (user == null || user.getStatut() == Utilisateur.Statut.banni) {
      revokeFamily(claimed.getFamilyId(), now);
      throw unauthorized();
    }

    TokenPair pair = issuePair(user, family, now);
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("tokenHash").is(submittedHash)),
        new Update().set("replacedByTokenHash", hash(pair.refreshToken())),
        RefreshTokenSession.class);

    RefreshTokenFamily currentFamily = families.findById(family.getId()).orElse(null);
    if (currentFamily == null || currentFamily.getRevokedAt() != null) {
      revokeFamily(family.getId(), Instant.now());
      throw unauthorized();
    }
    return pair;
  }

  public void revoke(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
    sessions.findByTokenHash(hash(rawRefreshToken))
        .ifPresent(session -> revokeFamily(session.getFamilyId(), Instant.now()));
  }

  private TokenPair issuePair(Utilisateur user, RefreshTokenFamily family, Instant now) {
    String refreshToken = randomToken();
    Instant refreshExpiresAt = now.plusSeconds(refreshTokenSeconds);
    if (refreshExpiresAt.isAfter(family.getExpiresAt())) {
      refreshExpiresAt = family.getExpiresAt();
    }

    RefreshTokenSession session = new RefreshTokenSession();
    session.setTokenHash(hash(refreshToken));
    session.setUserId(user.getId());
    session.setFamilyId(family.getId());
    session.setCreatedAt(now);
    session.setExpiresAt(refreshExpiresAt);
    sessions.save(session);

    return new TokenPair(createAccessToken(user, now), refreshToken,
        accessTokenSeconds, refreshExpiresAt);
  }

  private String createAccessToken(Utilisateur user, Instant now) {
    String role = user.getRole() != null ? user.getRole().name() : "client";
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("transport")
        .audience(List.of("transport-api"))
        .issuedAt(now)
        .expiresAt(now.plusSeconds(accessTokenSeconds))
        .subject(user.getEmail())
        .id(UUID.randomUUID().toString())
        .claim("uid", user.getId())
        .claim("purpose", "access")
        .claim("roles", List.of("ROLE_" + role.toUpperCase()))
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private void revokeFamily(String familyId, Instant now) {
    if (familyId == null) return;
    mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(familyId).and("revokedAt").is(null)),
        new Update().set("revokedAt", now),
        RefreshTokenFamily.class);
    mongoTemplate.updateMulti(
        Query.query(Criteria.where("familyId").is(familyId).and("revokedAt").is(null)),
        new Update().set("revokedAt", now),
        RefreshTokenSession.class);
  }

  private static String randomToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String hash(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 indisponible", impossible);
    }
  }

  private static ResponseStatusException unauthorized() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide ou expire");
  }

  public record TokenPair(
      String accessToken,
      String refreshToken,
      long expiresIn,
      Instant refreshExpiresAt) {
  }
}
