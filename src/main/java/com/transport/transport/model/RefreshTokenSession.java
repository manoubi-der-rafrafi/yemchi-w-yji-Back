package com.transport.transport.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "refresh_token_sessions")
@CompoundIndex(name = "refresh_family_created_idx", def = "{'familyId': 1, 'createdAt': -1}")
public class RefreshTokenSession {
  @Id
  private String id;

  @Indexed(unique = true)
  private String tokenHash;

  @Indexed
  private String userId;

  @Indexed
  private String familyId;

  private Instant createdAt;
  private Instant lastUsedAt;

  @Indexed(expireAfter = "0s")
  private Instant expiresAt;

  private Instant revokedAt;
  private String replacedByTokenHash;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getFamilyId() { return familyId; }
  public void setFamilyId(String familyId) { this.familyId = familyId; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getLastUsedAt() { return lastUsedAt; }
  public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
  public String getReplacedByTokenHash() { return replacedByTokenHash; }
  public void setReplacedByTokenHash(String replacedByTokenHash) { this.replacedByTokenHash = replacedByTokenHash; }
}
