package com.transport.transport.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.service.PresenceService;
import com.transport.transport.service.AuthorizationService;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

  private final PresenceService presence;
  private final AuthorizationService authorizationService;

  public PresenceController(PresenceService presence, AuthorizationService authorizationService) {
    this.presence = presence;
    this.authorizationService = authorizationService;
  }

  @PostMapping("/heartbeat")
public ResponseEntity<Void> heartbeat(
    Authentication auth
) {
  String principal = (auth != null && auth.isAuthenticated()
          && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))
          ? auth.getName()
          : null);

  presence.heartbeat(principal);
  return ResponseEntity.noContent().build();
}

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(Authentication auth) {
    String principal = (auth != null && auth.isAuthenticated()
        && !"anonymousUser".equals(String.valueOf(auth.getPrincipal())))
        ? auth.getName()
        : null;
    presence.logout(principal);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  public Map<String, Object> status(@PathVariable String id, Authentication authentication) {
    var current = authorizationService.currentUser(authentication);
    if (!authorizationService.canAccessUserData(current, id)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
    }
    boolean online = presence.isOnline(id);
    return Map.of("userId", id, "online", online);
  }
}
