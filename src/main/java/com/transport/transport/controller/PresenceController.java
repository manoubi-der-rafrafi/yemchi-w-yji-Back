package com.transport.transport.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.service.PresenceService;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

  private final PresenceService presence;

  public PresenceController(PresenceService presence) {
    this.presence = presence;
  }

  @PostMapping("/heartbeat")
public ResponseEntity<Void> heartbeat(
    Authentication auth,
    @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
) {
  String principal = (userIdHeader != null && !userIdHeader.isBlank())
      ? userIdHeader
      : (auth != null && auth.isAuthenticated()
          && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))
          ? auth.getName()
          : null);

  presence.heartbeat(principal);
  return ResponseEntity.noContent().build();
}

  @GetMapping("/{id}")
  public Map<String, Object> status(@PathVariable String id) {
    boolean online = presence.isOnline(id);
    return Map.of("userId", id, "online", online);
  }
}
