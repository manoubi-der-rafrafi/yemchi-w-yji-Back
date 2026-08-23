package com.transport.transport.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PasswordPolicy {
  public static final int MIN_LENGTH = 8;
  public static final int MAX_LENGTH = 128;

  private PasswordPolicy() {}

  public static void requireStrong(String password) {
    if (password == null || password.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe requis");
    }
    if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH
        || password.chars().noneMatch(Character::isUpperCase)
        || password.chars().noneMatch(Character::isLowerCase)
        || password.chars().noneMatch(Character::isDigit)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Le mot de passe doit contenir entre 8 et 128 caracteres, avec une majuscule, une minuscule et un chiffre");
    }
  }
}
