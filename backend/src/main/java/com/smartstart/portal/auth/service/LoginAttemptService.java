package com.smartstart.portal.auth.service;

import com.smartstart.portal.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login throttling: after {@code maxFailures} consecutive failures for a username
 * the account is locked for {@code lockout}. Suitable for the single-instance deployment in v1.
 */
@Service
public class LoginAttemptService {

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockout;
    private final Clock clock;

    public LoginAttemptService(AppProperties props, Clock clock) {
        this.maxFailures = props.login().maxFailures();
        this.lockout = props.login().lockout();
        this.clock = clock;
    }

    /** Returns the remaining lockout if the username is currently locked. */
    public Optional<Duration> lockedFor(String username) {
        Attempts a = attempts.get(key(username));
        if (a == null || a.lockedUntil == null) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        if (now.isBefore(a.lockedUntil)) {
            return Optional.of(Duration.between(now, a.lockedUntil));
        }
        attempts.remove(key(username));
        return Optional.empty();
    }

    public void recordFailure(String username) {
        attempts.compute(key(username), (k, a) -> {
            Attempts next = a == null ? new Attempts(0, null) : a;
            int failures = next.failures + 1;
            Instant lockedUntil = failures >= maxFailures ? clock.instant().plus(lockout) : null;
            return new Attempts(failures, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private record Attempts(int failures, Instant lockedUntil) {
    }
}
