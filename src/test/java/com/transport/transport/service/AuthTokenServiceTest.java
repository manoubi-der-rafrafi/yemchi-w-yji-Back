package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.RefreshTokenFamily;
import com.transport.transport.model.RefreshTokenSession;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.RefreshTokenFamilyRepository;
import com.transport.transport.repository.RefreshTokenSessionRepository;
import com.transport.transport.repository.UtilisateurRepository;

class AuthTokenServiceTest {
  private JwtEncoder encoder;
  private UtilisateurRepository users;
  private RefreshTokenFamilyRepository families;
  private RefreshTokenSessionRepository sessions;
  private MongoTemplate mongo;
  private AuthTokenService service;

  @BeforeEach
  void setUp() {
    encoder = mock(JwtEncoder.class);
    users = mock(UtilisateurRepository.class);
    families = mock(RefreshTokenFamilyRepository.class);
    sessions = mock(RefreshTokenSessionRepository.class);
    mongo = mock(MongoTemplate.class);
    Jwt jwt = mock(Jwt.class);
    when(jwt.getTokenValue()).thenReturn("access.jwt.token");
    when(encoder.encode(any())).thenReturn(jwt);
    when(families.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(sessions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service = new AuthTokenService(
        encoder, users, families, sessions, mongo, 900, 2_592_000, 7_776_000);
  }

  @Test
  void createSessionStoresOnlyAHashOfTheRefreshToken() {
    Utilisateur user = user();

    AuthTokenService.TokenPair pair = service.createSession(user);

    assertEquals("access.jwt.token", pair.accessToken());
    assertNotNull(pair.refreshToken());
    assertFalse(pair.refreshToken().isBlank());
    ArgumentCaptor<RefreshTokenSession> captor = ArgumentCaptor.forClass(RefreshTokenSession.class);
    verify(sessions).save(captor.capture());
    assertNotEquals(pair.refreshToken(), captor.getValue().getTokenHash());
    assertEquals(user.getId(), captor.getValue().getUserId());
  }

  @Test
  void rotateConsumesTheOldTokenAndReturnsANewOne() {
    Utilisateur user = user();
    RefreshTokenFamily family = family(user.getId());
    RefreshTokenSession claimed = new RefreshTokenSession();
    claimed.setUserId(user.getId());
    claimed.setFamilyId(family.getId());

    when(mongo.findAndModify(any(), any(), any(Class.class))).thenReturn(claimed);
    when(families.findById(family.getId())).thenReturn(Optional.of(family));
    when(users.findById(user.getId())).thenReturn(Optional.of(user));

    AuthTokenService.TokenPair pair = service.rotate("old-refresh-token");

    assertEquals("access.jwt.token", pair.accessToken());
    assertNotEquals("old-refresh-token", pair.refreshToken());
    verify(mongo).updateFirst(any(), any(), any(Class.class));
  }

  @Test
  void replayOfAConsumedTokenRevokesItsFamily() {
    RefreshTokenSession consumed = new RefreshTokenSession();
    consumed.setFamilyId("family-1");
    when(mongo.findAndModify(any(), any(), any(Class.class))).thenReturn(null);
    when(sessions.findByTokenHash(any())).thenReturn(Optional.of(consumed));

    ResponseStatusException exception = assertThrows(
        ResponseStatusException.class,
        () -> service.rotate("replayed-refresh-token"));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    verify(mongo).updateFirst(any(), any(), any(Class.class));
    verify(mongo).updateMulti(any(), any(), any(Class.class));
  }

  private static Utilisateur user() {
    Utilisateur user = new Utilisateur();
    user.setId("user-1");
    user.setEmail("user@example.com");
    user.setRole(Utilisateur.Role.client);
    user.setStatut(Utilisateur.Statut.actif);
    return user;
  }

  private static RefreshTokenFamily family(String userId) {
    RefreshTokenFamily family = new RefreshTokenFamily();
    family.setId("family-1");
    family.setUserId(userId);
    family.setCreatedAt(Instant.now());
    family.setExpiresAt(Instant.now().plusSeconds(7_776_000));
    return family;
  }
}
