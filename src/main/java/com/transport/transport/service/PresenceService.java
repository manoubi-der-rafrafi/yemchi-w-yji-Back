package com.transport.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.transport.transport.model.Utilisateur;
import com.transport.transport.model.Utilisateur.Statut;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class PresenceService {

  private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

  /** Timeout présence (en secondes), configurable : app.presence.timeout-seconds=40 */
  @Value("${app.presence.timeout-seconds:40}")
  private long timeoutSeconds;

  private final UtilisateurRepository repo;

  public PresenceService(UtilisateurRepository repo) { this.repo = repo; }

  /**
   * Marque un utilisateur "vivant" (lastSeen=now, online=true).
   * Le principal peut être un id OU un email (insensible à la casse).
   * @return true si un utilisateur a été mis à jour.
   */
  public boolean heartbeat(String principal) {
    if (principal == null || principal.isBlank()) {
      log.debug("heartbeat: principal NULL/blank → skip");
      return false;
    }

    Optional<Utilisateur> opt = repo.findById(principal)
        .or(() -> repo.findByEmailIgnoreCase(principal));

    if (opt.isEmpty()) {
      log.debug("heartbeat: user NOT FOUND for id/email='{}'", principal);
      return false;
    }

    Utilisateur u = opt.get();
    LocalDateTime now = LocalDateTime.now();

    boolean wasOnline = u.isOnline();
    // on met à jour lastSeen à chaque ping
    u.setLastSeen(now);

    // s'il était offline/expiré, on (re)passe online + statut actif (sauf banni)
    boolean expired = u.getLastSeen() == null || u.getLastSeen().isBefore(now.minusSeconds(timeoutSeconds));
    if (!wasOnline || expired) {
      u.setOnline(true);
      if (u.getStatut() != Statut.banni) {
        u.setStatut(Statut.actif);
      }
    }

    repo.save(u);
    log.debug("heartbeat: UPDATED user id={} online={} statut={}", u.getId(), u.isOnline(), u.getStatut());
    return true;
  }

  /** Calcul “à la volée” (et synchronise le booléen si nécessaire). */
  public boolean isOnline(String userId) {
    LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
    return repo.findById(userId)
        .map(u -> {
          boolean computed = u.getLastSeen() != null && u.getLastSeen().isAfter(cutoff);
          if (u.isOnline() != computed) {
            u.setOnline(computed);
            if (u.getStatut() != Statut.banni) {
              u.setStatut(computed ? Statut.actif : Statut.inactif);
            }
            repo.save(u);
          }
          return computed;
        })
        .orElse(false);
  }

  /** Batch pour l’UI : renvoie { id -> online } pour une liste d’ids. */
  public Map<String, Boolean> batchStatus(List<String> ids) {
    LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
    return repo.findAllById(ids).stream()
        .collect(Collectors.toMap(
            Utilisateur::getId,
            u -> u.getLastSeen() != null && u.getLastSeen().isAfter(cutoff)
        ));
  }

  /** Désactive automatiquement ceux dont lastSeen a expiré. */
  @Scheduled(fixedDelayString = "${app.presence.expire-delay-ms:30000}")
  public void expireInactives() {
    LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
    List<Utilisateur> toOff = repo.findAllByOnlineIsTrueAndLastSeenBefore(cutoff);
    if (toOff.isEmpty()) return;

    toOff.forEach(u -> {
      u.setOnline(false);
      if (u.getStatut() != Statut.banni) {
        u.setStatut(Statut.inactif);
      }
    });
    repo.saveAll(toOff);
    log.debug("expireInactives: OFF {}", toOff.stream().map(Utilisateur::getId).toList());
  }
}
