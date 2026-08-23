package com.transport.transport.service;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/** Limiteur a fenetre fixe, adapte au deploiement API mono-instance actuel. */
@Service
public class RequestRateLimiter {
  private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
  private final AtomicLong requests = new AtomicLong();
  private final Clock clock;

  public RequestRateLimiter() {
    this(Clock.systemUTC());
  }

  RequestRateLimiter(Clock clock) {
    this.clock = clock;
  }

  public Result acquire(String scope, String subject, int limit, long windowSeconds) {
    int safeLimit = Math.max(1, limit);
    long safeWindow = Math.max(1, windowSeconds);
    long nowSeconds = clock.instant().getEpochSecond();
    long window = nowSeconds / safeWindow;
    String key = scope + ':' + subject;

    Counter counter = counters.compute(key, (ignored, current) ->
        current == null || current.window() != window
            ? new Counter(window, 1)
            : new Counter(window, current.count() + 1));

    if ((requests.incrementAndGet() & 1023L) == 0L) {
      counters.entrySet().removeIf(entry -> entry.getValue().window() < window - 2);
    }

    long retryAfter = Math.max(1, ((window + 1) * safeWindow) - nowSeconds);
    return new Result(counter.count() <= safeLimit, retryAfter);
  }

  public record Result(boolean allowed, long retryAfterSeconds) {}
  private record Counter(long window, int count) {}
}
