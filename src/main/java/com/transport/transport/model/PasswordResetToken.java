package com.transport.transport.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Jeton opaque de reinitialisation. La valeur brute n'est jamais stockee. */
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
  /** SHA-256 du jeton brut. L'identifiant Mongo garantit son unicite. */
  @Id
  private String tokenHash;

  @Indexed
  private String userId;

  private Instant createdAt;

  /** TTL pour le nettoyage; l'expiration est aussi verifiee dans les requetes. */
  @Indexed(expireAfter = "0s")
  private Instant expiresAt;

  private Instant consumedAt;

  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getConsumedAt() { return consumedAt; }
  public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
}
