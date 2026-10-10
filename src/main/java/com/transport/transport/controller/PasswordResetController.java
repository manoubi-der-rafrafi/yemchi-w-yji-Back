package com.transport.transport.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.service.PasswordResetService;

@RestController
@RequestMapping("/api/utilisateur")
public class PasswordResetController {
  private static final Map<String, String> GENERIC_RESPONSE = Map.of(
      "message", "Si un compte correspond a cette adresse, un email a ete envoye");

  private final PasswordResetService passwordResetService;

  public PasswordResetController(PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@RequestBody(required = false) ForgotPasswordRequest request) {
    try {
      passwordResetService.requestReset(request == null ? null : request.email());
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(GENERIC_RESPONSE);
    } catch (ResponseStatusException error) {
      return ResponseEntity.status(error.getStatusCode())
          .body(Map.of("error", error.getReason() == null ? "Requete invalide" : error.getReason()));
    }
  }

  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody(required = false) ResetPasswordRequest request) {
    try {
      passwordResetService.resetPassword(
          request == null ? null : request.token(),
          request == null ? null : request.newPassword());
      return ResponseEntity.ok(Map.of("message", "Mot de passe modifie avec succes"));
    } catch (ResponseStatusException error) {
      return ResponseEntity.status(error.getStatusCode())
          .body(Map.of("error", error.getReason() == null ? "Requete invalide" : error.getReason()));
    }
  }

  public record ForgotPasswordRequest(String email) {}
  public record ResetPasswordRequest(String token, String newPassword) {}
}
