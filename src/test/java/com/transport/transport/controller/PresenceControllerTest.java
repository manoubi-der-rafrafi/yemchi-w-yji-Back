package com.transport.transport.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.PresenceService;

class PresenceControllerTest {

  private PresenceService presenceService;
  private AuthorizationService authorizationService;
  private PresenceController controller;
  private Authentication authentication;
  private Utilisateur current;

  @BeforeEach
  void setUp() {
    presenceService = mock(PresenceService.class);
    authorizationService = mock(AuthorizationService.class);
    authentication = mock(Authentication.class);
    current = new Utilisateur();
    current.setId("client-1");
    current.setRole(Utilisateur.Role.client);
    when(authorizationService.currentUser(authentication)).thenReturn(current);
    controller = new PresenceController(presenceService, authorizationService);
  }

  @Test
  void refuseLeStatutEnLigneDUnUtilisateurSansRelation() {
    when(authorizationService.canAccessUserData(current, "other-client")).thenReturn(false);

    assertThrows(
        ResponseStatusException.class,
        () -> controller.status("other-client", authentication));

    verifyNoInteractions(presenceService);
  }

  @Test
  void autoriseLeStatutEnLigneDUnUtilisateurLie() {
    when(authorizationService.canAccessUserData(current, "friend-client")).thenReturn(true);
    when(presenceService.isOnline("friend-client")).thenReturn(true);

    var response = controller.status("friend-client", authentication);

    assertEquals("friend-client", response.get("userId"));
    assertEquals(true, response.get("online"));
  }
}
