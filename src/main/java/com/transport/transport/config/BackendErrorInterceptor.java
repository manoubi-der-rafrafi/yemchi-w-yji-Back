package com.transport.transport.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.transport.transport.dto.ApplicationErrorRequest;
import com.transport.transport.service.ApplicationErrorService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BackendErrorInterceptor implements HandlerInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(BackendErrorInterceptor.class);

  private final ApplicationErrorService errors;

  public BackendErrorInterceptor(ApplicationErrorService errors) {
    this.errors = errors;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      Exception exception) {
    if ((exception == null && response.getStatus() < 500) || "/api/errors".equals(request.getRequestURI())) {
      return;
    }

    try {
      String principal = request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();
      String message = exception == null
          ? "HTTP " + response.getStatus() + " " + request.getMethod() + " " + request.getRequestURI()
          : exception.getMessage();
      if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();

      errors.collect(
          new ApplicationErrorRequest(
              message,
              exception == null ? "backend_http_error" : exception.getClass().getSimpleName(),
              "backend",
              response.getStatus() >= 500 ? "error" : "warning",
              null,
              request.getRequestURI(),
              response.getStatus(),
              exception == null ? null : stackTrace(exception),
              Map.of("method", request.getMethod()),
              null,
              "server"),
          principal,
          request.getHeader("User-Agent"));
    } catch (RuntimeException loggingFailure) {
      logger.warn("Application error collection failed: {}", loggingFailure.getMessage());
    }
  }

  private String stackTrace(Exception exception) {
    StringBuilder result = new StringBuilder();
    result.append(exception).append('\n');
    for (StackTraceElement element : exception.getStackTrace()) {
      if (result.length() >= 6000) break;
      result.append("at ").append(element).append('\n');
    }
    return result.toString();
  }
}
