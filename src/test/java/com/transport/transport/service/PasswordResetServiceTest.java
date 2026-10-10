package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.PasswordResetToken;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.PasswordResetTokenRepository;
import com.transport.transport.repository.UtilisateurRepository;

class PasswordResetServiceTest {
  private UtilisateurRepository users;
  private PasswordResetTokenRepository tokens;
  private MongoTemplate mongo;
  private PasswordEncoder encoder;
  private MailService mail;
  private AuthTokenService authTokens;
  private PasswordResetService service;

  @BeforeEach
  void setUp() {
    users = mock(UtilisateurRepository.class);
    tokens = mock(PasswordResetTokenRepository.class);
    mongo = mock(MongoTemplate.class);
    encoder = mock(PasswordEncoder.class);
    mail = mock(MailService.class);
    authTokens = mock(AuthTokenService.class);
    when(tokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service = new PasswordResetService(
        users, tokens, mongo, encoder, mail, authTokens,
        "yemchiwyji://reset-password", 900);
  }

  @Test
  void requestStoresOnlyHashAndSendsRawTokenInLink() {
    Utilisateur user = user();
    when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

    service.requestReset(" User@Example.com ");

    ArgumentCaptor<PasswordResetToken> stored = ArgumentCaptor.forClass(PasswordResetToken.class);
    ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
    verify(tokens).save(stored.capture());
    verify(mail).sendPasswordResetEmail(
        org.mockito.ArgumentMatchers.eq("user@example.com"), link.capture(),
        org.mockito.ArgumentMatchers.eq(15L));
    String rawToken = link.getValue().substring(link.getValue().indexOf("token=") + 6);
    assertNotEquals(rawToken, stored.getValue().getTokenHash());
    assertEquals(PasswordResetService.hash(rawToken), stored.getValue().getTokenHash());
    assertEquals(user.getId(), stored.getValue().getUserId());
  }

  @Test
  void unknownEmailReturnsNormallyWithoutSendingAnything() {
    when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

    service.requestReset("missing@example.com");

    verify(tokens, never()).save(any());
    verify(mail, never()).sendPasswordResetEmail(any(), any(), any(Long.class));
  }

  @Test
  void resetConsumesTokenHashesPasswordAndRevokesSessions() {
    Utilisateur user = user();
    PasswordResetToken token = validToken(user.getId());
    when(tokens.findById(PasswordResetService.hash("raw-reset-token")))
        .thenReturn(Optional.of(token));
    when(users.findById(user.getId())).thenReturn(Optional.of(user));
    when(encoder.matches("NewPassword2026", user.getMotDePasse())).thenReturn(false);
    when(encoder.encode("NewPassword2026")).thenReturn("{bcrypt}new-hash");
    when(mongo.findAndModify(any(), any(), any(Class.class))).thenReturn(token);

    service.resetPassword("raw-reset-token", "NewPassword2026");

    ArgumentCaptor<Utilisateur> saved = ArgumentCaptor.forClass(Utilisateur.class);
    verify(users).save(saved.capture());
    assertEquals("{bcrypt}new-hash", saved.getValue().getMotDePasse());
    verify(authTokens).revokeAllForUser(user.getId());
  }

  @Test
  void consumedTokenIsRejectedWithoutChangingPassword() {
    PasswordResetToken token = validToken("user-1");
    token.setConsumedAt(Instant.now());
    when(tokens.findById(PasswordResetService.hash("used-token")))
        .thenReturn(Optional.of(token));

    ResponseStatusException error = assertThrows(
        ResponseStatusException.class,
        () -> service.resetPassword("used-token", "NewPassword2026"));

    assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    verify(users, never()).save(any());
  }

  @Test
  void cleanupRemovesExpiredTokenDocuments() {
    service.cleanupExpiredTokens();

    verify(mongo).remove(
        any(org.springframework.data.mongodb.core.query.Query.class),
        org.mockito.ArgumentMatchers.eq(PasswordResetToken.class));
  }

  private static Utilisateur user() {
    Utilisateur user = new Utilisateur();
    user.setId("user-1");
    user.setEmail("user@example.com");
    user.setMotDePasse("{bcrypt}old-hash");
    user.setStatut(Utilisateur.Statut.actif);
    return user;
  }

  private static PasswordResetToken validToken(String userId) {
    PasswordResetToken token = new PasswordResetToken();
    token.setTokenHash(PasswordResetService.hash("raw-reset-token"));
    token.setUserId(userId);
    token.setCreatedAt(Instant.now().minusSeconds(30));
    token.setExpiresAt(Instant.now().plusSeconds(600));
    return token;
  }
}
