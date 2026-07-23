package com.transport.transport.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.dto.ApplicationErrorRequest;
import com.transport.transport.model.ApplicationError;
import com.transport.transport.service.ApplicationErrorService;

@RestController
@RequestMapping("/api")
public class ApplicationErrorController {
  private final ApplicationErrorService errors;

  public ApplicationErrorController(ApplicationErrorService errors) {
    this.errors = errors;
  }

  @PostMapping("/errors")
  public ResponseEntity<Void> collect(
      @RequestBody ApplicationErrorRequest request,
      Authentication authentication,
      @RequestHeader(value = "User-Agent", required = false) String userAgent) {
    String principal = authentication != null && authentication.isAuthenticated()
        && !"anonymousUser".equals(String.valueOf(authentication.getPrincipal()))
            ? authentication.getName()
            : null;
    errors.collect(request, principal, userAgent);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/admin/errors")
  @PreAuthorize("hasAnyAuthority('admin', 'ROLE_admin', 'ADMIN', 'ROLE_ADMIN')")
  public Page<ApplicationError> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return errors.list(page, size);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
  }
}
