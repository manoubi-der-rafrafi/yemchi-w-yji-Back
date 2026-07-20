package com.transport.transport.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_position_history")
@CompoundIndex(name = "user_position_time_idx", def = "{'userId': 1, 'collectedAt': -1}")
public class UserPositionHistory {
  @Id
  private String id;
  private String userId;
  private Utilisateur.Role role;
  private String appType;

  @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
  private GeoJsonPoint location;

  private Double accuracyMeters;
  private Double speedMetersPerSecond;
  private Double headingDegrees;
  private Double altitudeMeters;
  private Instant collectedAt;
  private Instant receivedAt;
  private String deviceId;
  private String commandeId;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public Utilisateur.Role getRole() { return role; }
  public void setRole(Utilisateur.Role role) { this.role = role; }
  public String getAppType() { return appType; }
  public void setAppType(String appType) { this.appType = appType; }
  public GeoJsonPoint getLocation() { return location; }
  public void setLocation(GeoJsonPoint location) { this.location = location; }
  public Double getAccuracyMeters() { return accuracyMeters; }
  public void setAccuracyMeters(Double accuracyMeters) { this.accuracyMeters = accuracyMeters; }
  public Double getSpeedMetersPerSecond() { return speedMetersPerSecond; }
  public void setSpeedMetersPerSecond(Double speedMetersPerSecond) { this.speedMetersPerSecond = speedMetersPerSecond; }
  public Double getHeadingDegrees() { return headingDegrees; }
  public void setHeadingDegrees(Double headingDegrees) { this.headingDegrees = headingDegrees; }
  public Double getAltitudeMeters() { return altitudeMeters; }
  public void setAltitudeMeters(Double altitudeMeters) { this.altitudeMeters = altitudeMeters; }
  public Instant getCollectedAt() { return collectedAt; }
  public void setCollectedAt(Instant collectedAt) { this.collectedAt = collectedAt; }
  public Instant getReceivedAt() { return receivedAt; }
  public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
  public String getCommandeId() { return commandeId; }
  public void setCommandeId(String commandeId) { this.commandeId = commandeId; }
}
