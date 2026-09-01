package com.example.online_workspace.services.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * メールアドレスと接続元ごとの連続ログイン失敗を制限する。
 */
@Component
public class LoginRateLimiter {

	public static final int MAX_FAILURES = 5;
	private static final Duration WINDOW = Duration.ofMinutes(15);
	private static final int MAX_TRACKED_KEYS = 10_000;
	public static final long RETRY_AFTER_SECONDS = WINDOW.toSeconds();

	// ponytail: 単一インスタンス向け。複数インスタンス化するときは共有ストアへ移す。
	private final Map<String, FailedAttempts> attempts = new LinkedHashMap<>(16, 0.75f, true);
	private final Clock clock;

	public LoginRateLimiter() {
		this(Clock.systemUTC());
	}

	LoginRateLimiter(Clock clock) {
		this.clock = clock;
	}

	public synchronized boolean isBlocked(String email, String clientAddress) {
		Instant now = clock.instant();
		removeExpired(now);
		FailedAttempts failed = attempts.get(key(email, clientAddress));
		return failed != null && failed.blockedUntil != null && failed.blockedUntil.isAfter(now);
	}

	public synchronized void recordFailure(String email, String clientAddress) {
		Instant now = clock.instant();
		removeExpired(now);
		String key = key(email, clientAddress);
		if (!attempts.containsKey(key) && attempts.size() >= MAX_TRACKED_KEYS) {
			Iterator<String> oldest = attempts.keySet().iterator();
			oldest.next();
			oldest.remove();
		}
		FailedAttempts failed = attempts.computeIfAbsent(key, ignored -> new FailedAttempts());
		failed.count++;
		failed.lastFailureAt = now;
		if (failed.count >= MAX_FAILURES) {
			failed.blockedUntil = now.plus(WINDOW);
		}
	}

	public synchronized void reset(String email, String clientAddress) {
		attempts.remove(key(email, clientAddress));
	}

	private void removeExpired(Instant now) {
		attempts.values().removeIf(failed -> {
			Instant expiresAt = failed.blockedUntil == null
				? failed.lastFailureAt.plus(WINDOW)
				: failed.blockedUntil;
			return !expiresAt.isAfter(now);
		});
	}

	private String key(String email, String clientAddress) {
		return email + '\0' + clientAddress;
	}

	private static final class FailedAttempts {
		private int count;
		private Instant lastFailureAt;
		private Instant blockedUntil;
	}
}
