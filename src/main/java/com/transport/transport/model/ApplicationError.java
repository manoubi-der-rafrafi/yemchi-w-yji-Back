package com.transport.transport.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "application_error")
@CompoundIndex(def = "{'source': 1, 'createdAt': -1}")
@CompoundIndex(def = "{'severity': 1, 'createdAt': -1}")
public class ApplicationError {
  @Id
  private String id;

  private String message;
  private String type;
  private String source;
  private String severity;

  @Indexed
  private String userId;
  private String userEmail;
  private String userName;
  private String role;
  private String page;
  private String endpoint;
  private Integer httpStatus;
  private String stackTrace;
  private Map<String, Object> metadata;
  private String appVersion;
  private String deviceType;
  private String userAgent;

  @Indexed
  private Instant createdAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public String getSeverity() { return severity; }
  public void setSeverity(String severity) { this.severity = severity; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getUserEmail() { return userEmail; }
  public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getPage() { return page; }
  public void setPage(String page) { this.page = page; }
  public String getEndpoint() { return endpoint; }
  public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
  public Integer getHttpStatus() { return httpStatus; }
  public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
  public String getStackTrace() { return stackTrace; }
  public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
  public Map<String, Object> getMetadata() { return metadata; }
  public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
  public String getAppVersion() { return appVersion; }
  public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
  public String getDeviceType() { return deviceType; }
  public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
  public String getUserAgent() { return userAgent; }
  public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
