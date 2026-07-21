package com.transport.transport.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "analytics_event")
@CompoundIndex(name = "analytics_platform_event_time_idx", def = "{'platform': 1, 'eventName': 1, 'createdAt': -1}")
public class AnalyticsEvent {
  @Id
  private String id;

  @Indexed
  private String visitorId;
  private String sessionId;
  private String installationId;
  private String userId;
  private String platform;
  private String eventName;
  private String page;
  private Map<String, Object> metadata;
  private Instant createdAt;
  private String deviceType;
  private String userAgent;
  private String appVersion;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getVisitorId() { return visitorId; }
  public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
  public String getSessionId() { return sessionId; }
  public void setSessionId(String sessionId) { this.sessionId = sessionId; }
  public String getInstallationId() { return installationId; }
  public void setInstallationId(String installationId) { this.installationId = installationId; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getPlatform() { return platform; }
  public void setPlatform(String platform) { this.platform = platform; }
  public String getEventName() { return eventName; }
  public void setEventName(String eventName) { this.eventName = eventName; }
  public String getPage() { return page; }
  public void setPage(String page) { this.page = page; }
  public Map<String, Object> getMetadata() { return metadata; }
  public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public String getDeviceType() { return deviceType; }
  public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
  public String getUserAgent() { return userAgent; }
  public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
  public String getAppVersion() { return appVersion; }
  public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
}
