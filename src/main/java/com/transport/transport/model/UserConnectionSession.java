package com.transport.transport.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_connection_session")
@CompoundIndex(name = "user_connection_time_idx", def = "{'userId': 1, 'connectedAt': -1}")
public class UserConnectionSession {
  public enum EndReason { LOGOUT, TIMEOUT, TOKEN_EXPIRED, APP_CLOSED, CONNECTION_LOST, FORCED_BY_ADMIN }

  @Id
  private String id;
  private String userId;
  private Utilisateur.Role role;
  private String appType;
  private String deviceId;
  private String platform;
  private String appVersion;
  private Instant connectedAt;
  private Instant lastHeartbeatAt;
  private Instant disconnectedAt;
  private EndReason endReason;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public Utilisateur.Role getRole() { return role; }
  public void setRole(Utilisateur.Role role) { this.role = role; }
  public String getAppType() { return appType; }
  public void setAppType(String appType) { this.appType = appType; }
  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
  public String getPlatform() { return platform; }
  public void setPlatform(String platform) { this.platform = platform; }
  public String getAppVersion() { return appVersion; }
  public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
  public Instant getConnectedAt() { return connectedAt; }
  public void setConnectedAt(Instant connectedAt) { this.connectedAt = connectedAt; }
  public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
  public void setLastHeartbeatAt(Instant lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }
  public Instant getDisconnectedAt() { return disconnectedAt; }
  public void setDisconnectedAt(Instant disconnectedAt) { this.disconnectedAt = disconnectedAt; }
  public EndReason getEndReason() { return endReason; }
  public void setEndReason(EndReason endReason) { this.endReason = endReason; }
}
