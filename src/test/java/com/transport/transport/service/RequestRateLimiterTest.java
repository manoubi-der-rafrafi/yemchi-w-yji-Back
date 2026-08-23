package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {
  @Test
  void blocksOnlyAfterLimitAndSeparatesSubjects() {
    RequestRateLimiter limiter = new RequestRateLimiter(
        Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC));

    assertTrue(limiter.acquire("login", "10.0.0.1", 2, 60).allowed());
    assertTrue(limiter.acquire("login", "10.0.0.1", 2, 60).allowed());
    var blocked = limiter.acquire("login", "10.0.0.1", 2, 60);
    assertFalse(blocked.allowed());
    assertTrue(blocked.retryAfterSeconds() > 0);
    assertTrue(limiter.acquire("login", "10.0.0.2", 2, 60).allowed());
  }
}
