package com.example.online_workspace.services.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTests {

	@Test
	void failureCountExpiresAfterTheConsecutiveFailureWindow() {
		MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
		LoginRateLimiter limiter = new LoginRateLimiter(clock);

		for (int attempt = 1; attempt <= 5; attempt++) {
			limiter.recordFailure("user@example.com", "198.51.100.10");
		}
		assertTrue(limiter.isBlocked("user@example.com", "198.51.100.10"));

		clock.advance(Duration.ofMinutes(15).plusSeconds(1));

		assertFalse(limiter.isBlocked("user@example.com", "198.51.100.10"));
	}

	@Test
	void failuresBeforeTheWindowDoNotCountAsConsecutive() {
		MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
		LoginRateLimiter limiter = new LoginRateLimiter(clock);

		for (int attempt = 1; attempt <= 4; attempt++) {
			limiter.recordFailure("user@example.com", "198.51.100.10");
		}
		clock.advance(Duration.ofMinutes(15).plusSeconds(1));
		limiter.recordFailure("user@example.com", "198.51.100.10");

		assertFalse(limiter.isBlocked("user@example.com", "198.51.100.10"));
	}

	private static final class MutableClock extends Clock {

		private Instant current;

		private MutableClock(Instant current) {
			this.current = current;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return current;
		}

		private void advance(Duration duration) {
			current = current.plus(duration);
		}
	}
}
