package com.transport.transport.service;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiRequestRateLimiter {
    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requests = new AtomicLong();
    private final Clock clock;

    public AiRequestRateLimiter() {
        this(Clock.systemUTC());
    }

    AiRequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(String operation, String principal, int limitPerMinute) {
        int safeLimit = Math.max(1, limitPerMinute);
        long currentWindow = clock.millis() / WINDOW_MILLIS;
        String key = operation + ':' + principal;
        AtomicBoolean exceeded = new AtomicBoolean(false);

        counters.compute(key, (ignored, current) -> {
            Counter next = current == null || current.window() != currentWindow
                    ? new Counter(currentWindow, 1)
                    : new Counter(currentWindow, current.count() + 1);
            exceeded.set(next.count() > safeLimit);
            return next;
        });

        if ((requests.incrementAndGet() & 1023L) == 0L) {
            counters.entrySet().removeIf(entry -> entry.getValue().window() < currentWindow - 1);
        }

        if (exceeded.get()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de demandes d'analyse. Reessayez dans une minute.");
        }
    }

    private record Counter(long window, int count) {}
}
