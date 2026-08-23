package com.transport.transport.controller;

import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiConcurrencyExceptionHandler {
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<Map<String, String>> conflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
        "error", "La ressource a ete modifiee par une autre operation. Rechargez puis reessayez."));
  }
}
