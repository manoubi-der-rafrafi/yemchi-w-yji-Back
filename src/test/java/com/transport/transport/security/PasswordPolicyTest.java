package com.transport.transport.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PasswordPolicyTest {
  @Test
  void acceptsStrongPassword() {
    assertDoesNotThrow(() -> PasswordPolicy.requireStrong("Yemchi2026"));
  }

  @Test
  void rejectsShortOrSingleCasePasswords() {
    assertThrows(ResponseStatusException.class, () -> PasswordPolicy.requireStrong("Abc123"));
    assertThrows(ResponseStatusException.class, () -> PasswordPolicy.requireStrong("yemchi2026"));
    assertThrows(ResponseStatusException.class, () -> PasswordPolicy.requireStrong("YEMCHI2026"));
    assertThrows(ResponseStatusException.class, () -> PasswordPolicy.requireStrong("YemchiTest"));
  }
}
