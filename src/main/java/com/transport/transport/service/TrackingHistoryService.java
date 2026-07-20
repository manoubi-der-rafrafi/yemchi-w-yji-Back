package com.transport.transport.service;

import java.time.Instant;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import com.transport.transport.model.UserConnectionSession;
import com.transport.transport.model.UserPositionHistory;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UserConnectionSessionRepository;
import com.transport.transport.repository.UserPositionHistoryRepository;

@Service
public class TrackingHistoryService {
  private final UserPositionHistoryRepository positionRepository;
  private final UserConnectionSessionRepository connectionRepository;

  public TrackingHistoryService(
      UserPositionHistoryRepository positionRepository,
      UserConnectionSessionRepository connectionRepository) {
    this.positionRepository = positionRepository;
    this.connectionRepository = connectionRepository;
  }

  public void recordPosition(
      Utilisateur user,
      double latitude,
      double longitude,
      Double accuracyMeters,
      Double speedMetersPerSecond,
      Double headingDegrees,
      Double altitudeMeters,
      Instant collectedAt,
      String deviceId,
      String commandeId) {
    if (user.getRole() == Utilisateur.Role.admin) return;

    UserPositionHistory position = new UserPositionHistory();
    position.setUserId(user.getId());
    position.setRole(user.getRole());
    position.setAppType(appType(user.getRole()));
    position.setLocation(new GeoJsonPoint(longitude, latitude));
    position.setAccuracyMeters(accuracyMeters);
    position.setSpeedMetersPerSecond(speedMetersPerSecond);
    position.setHeadingDegrees(headingDegrees);
    position.setAltitudeMeters(altitudeMeters);
    position.setCollectedAt(collectedAt != null ? collectedAt : Instant.now());
    position.setReceivedAt(Instant.now());
    position.setDeviceId(blankToNull(deviceId));
    position.setCommandeId(blankToNull(commandeId));
    positionRepository.save(position);
  }

  public void heartbeat(Utilisateur user) {
    if (user.getRole() == Utilisateur.Role.admin) return;
    Instant now = Instant.now();
    UserConnectionSession session = connectionRepository
        .findFirstByUserIdAndDisconnectedAtIsNullOrderByConnectedAtDesc(user.getId())
        .orElseGet(() -> {
          UserConnectionSession created = new UserConnectionSession();
          created.setUserId(user.getId());
          created.setRole(user.getRole());
          created.setAppType(appType(user.getRole()));
          created.setConnectedAt(now);
          return created;
        });
    session.setLastHeartbeatAt(now);
    connectionRepository.save(session);
  }

  public void closeSession(String userId, UserConnectionSession.EndReason reason) {
    connectionRepository.findFirstByUserIdAndDisconnectedAtIsNullOrderByConnectedAtDesc(userId)
        .ifPresent(session -> {
          Instant now = Instant.now();
          session.setLastHeartbeatAt(session.getLastHeartbeatAt() != null ? session.getLastHeartbeatAt() : now);
          session.setDisconnectedAt(now);
          session.setEndReason(reason);
          connectionRepository.save(session);
        });
  }

  private String appType(Utilisateur.Role role) {
    return role == Utilisateur.Role.transporteur ? "LIVREUR" : "CLIENT";
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
