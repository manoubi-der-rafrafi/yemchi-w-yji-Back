package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AiRequestRateLimiterTest {

    @Test
    void rejectsRequestsAboveThePerUserLimit() {
        AiRequestRateLimiter limiter = new AiRequestRateLimiter(
                Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneOffset.UTC));

        assertDoesNotThrow(() -> limiter.check("image", "user@example.com", 2));
        assertDoesNotThrow(() -> limiter.check("image", "user@example.com", 2));
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> limiter.check("image", "user@example.com", 2));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
    }
}
