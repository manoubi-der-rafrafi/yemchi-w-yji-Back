package com.transport.transport.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.service.PasswordResetService;

class PasswordResetControllerTest {
  private PasswordResetService service;
  private PasswordResetController controller;

  @BeforeEach
  void setUp() {
    service = mock(PasswordResetService.class);
    controller = new PasswordResetController(service);
  }

  @Test
  void forgotPasswordAlwaysReturnsGenericAcceptedResponse() {
    var response = controller.forgotPassword(
        new PasswordResetController.ForgotPasswordRequest("user@example.com"));

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    verify(service).requestReset("user@example.com");
  }

  @Test
  void resetMapsInvalidTokenWithoutLeakingDetails() {
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide"))
        .when(service).resetPassword("bad-token", "NewPassword2026");

    var response = controller.resetPassword(
        new PasswordResetController.ResetPasswordRequest("bad-token", "NewPassword2026"));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }
}
