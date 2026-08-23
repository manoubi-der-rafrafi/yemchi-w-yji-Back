package com.transport.transport.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "signup_verification_sessions")
public class SignupVerificationSession {
  @Id private String id;
  @Indexed private String userId;
  @Indexed private String email;
  @Indexed(unique = true) private String clientTokenHash;
  private Instant createdAt;
  @Indexed(expireAfter = "0s") private Instant expiresAt;
  private Instant verifiedAt;
  private Instant consumedAt;
  @Version private Long version;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getClientTokenHash() { return clientTokenHash; }
  public void setClientTokenHash(String value) { this.clientTokenHash = value; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant value) { this.createdAt = value; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant value) { this.expiresAt = value; }
  public Instant getVerifiedAt() { return verifiedAt; }
  public void setVerifiedAt(Instant value) { this.verifiedAt = value; }
  public Instant getConsumedAt() { return consumedAt; }
  public void setConsumedAt(Instant value) { this.consumedAt = value; }
  public Long getVersion() { return version; }
  public void setVersion(Long value) { this.version = value; }
}
