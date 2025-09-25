package com.transport.transport.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.transport.transport.service.PresenceService;

@Component
public class PresenceScheduler {
  private final PresenceService presenceService;

  public PresenceScheduler(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  // Toutes les 30s : expire les utilisateurs inactifs
  @Scheduled(fixedDelay = 30000)
  public void expireJob() {
    presenceService.expireInactives();
  }
}
