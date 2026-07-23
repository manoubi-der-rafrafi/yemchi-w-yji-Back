package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.transport.transport.dto.ApplicationErrorRequest;
import com.transport.transport.model.ApplicationError;
import com.transport.transport.repository.ApplicationErrorRepository;
import com.transport.transport.repository.UtilisateurRepository;

class ApplicationErrorServiceTest {
  @Test
  void removesSensitiveDataBeforeSaving() {
    ApplicationErrorRepository errors = mock(ApplicationErrorRepository.class);
    UtilisateurRepository users = mock(UtilisateurRepository.class);
    ApplicationErrorService service = new ApplicationErrorService(errors, users);

    service.collect(
        new ApplicationErrorRequest(
            "request failed Authorization: Bearer secret-token password=hunter2",
            "http_error",
            "mobile_client",
            "error",
            "/checkout?token=visible",
            "/api/commandes?accessToken=visible",
            500,
            "token=abc.def.ghi",
            Map.of(
                "method", "POST",
                "password", "hunter2",
                "authorization", "Bearer secret-token",
                "coordinates", "36.8,10.1",
                "context", Map.of("token", "nested-secret", "step", "checkout")),
            "1.0.0",
            "android"),
        null,
        "test-agent");

    ArgumentCaptor<ApplicationError> captor = ArgumentCaptor.forClass(ApplicationError.class);
    verify(errors).save(captor.capture());
    ApplicationError saved = captor.getValue();

    assertFalse(saved.getMessage().contains("secret-token"));
    assertFalse(saved.getMessage().contains("hunter2"));
    assertEquals("/checkout", saved.getPage());
    assertEquals("/api/commandes", saved.getEndpoint());
    assertFalse(saved.getMetadata().containsKey("password"));
    assertFalse(saved.getMetadata().containsKey("authorization"));
    assertFalse(saved.getMetadata().containsKey("coordinates"));
    assertEquals("POST", saved.getMetadata().get("method"));
    assertTrue(saved.getMetadata().containsKey("context"));
    assertFalse(String.valueOf(saved.getMetadata().get("context")).contains("nested-secret"));
    assertNull(saved.getUserId());
  }
}
