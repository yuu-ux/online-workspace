package com.example.online_workspace.services.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * ログイン失敗の連続試行をアプリケーション単位で制限する。
 *
 * 現段階では単一アプリケーションインスタンス向けのインメモリ実装とし、
 * 将来複数インスタンス化する場合は共有ストアへ差し替える。
 */
@Component
public class LoginRateLimiter {

	public static final int MAX_FAILURES = 5;
	private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
	private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
	private static final int MAX_TRACKED_KEYS = 10_000;
	public static final long BLOCK_DURATION_SECONDS = BLOCK_DURATION.toSeconds();

	private final Map<String, FailedAttempts> failedAttempts = new LinkedHashMap<>(16, 0.75f, true);
	private final Clock clock;

	public LoginRateLimiter() {
		this(Clock.systemUTC());
	}

	LoginRateLimiter(Clock clock) {
		this.clock = clock;
	}

	/**
	 * ログイン試行がブロック中か確認する。
	 *
	 * @param email 正規化済みメールアドレス
	 * @param clientAddress 接続元アドレス
	 * @return ブロック中の場合はtrue
	 */
	public synchronized boolean isBlocked(String email, String clientAddress) {
		Instant now = clock.instant();
		removeExpired(now);
		FailedAttempts attempts = failedAttempts.get(key(email, clientAddress));
		if (attempts == null) {
			return false;
		}
		if (attempts.blockedUntil != null) {
			return true;
		}
		if (attempts.count >= MAX_FAILURES) {
			attempts.blockedUntil = now.plus(BLOCK_DURATION);
			return true;
		}
		return false;
	}

	/**
	 * ログイン失敗を記録する。
	 *
	 * @param email 正規化済みメールアドレス
	 * @param clientAddress 接続元アドレス
	 */
	public synchronized void recordFailure(String email, String clientAddress) {
		Instant now = clock.instant();
		removeExpired(now);
		String key = key(email, clientAddress);
		FailedAttempts attempts = failedAttempts.get(key);
		if (attempts == null) {
			ensureCapacity();
			attempts = new FailedAttempts();
			failedAttempts.put(key, attempts);
		}
		attempts.count++;
		attempts.lastFailureAt = now;
	}

	/**
	 * 成功したログインの失敗状態を消去する。
	 *
	 * @param email 正規化済みメールアドレス
	 * @param clientAddress 接続元アドレス
	 */
	public synchronized void reset(String email, String clientAddress) {
		failedAttempts.remove(key(email, clientAddress));
	}

	private void removeExpired(Instant now) {
		Iterator<Map.Entry<String, FailedAttempts>> iterator = failedAttempts.entrySet().iterator();
		while (iterator.hasNext()) {
			FailedAttempts attempts = iterator.next().getValue();
			if (attempts.blockedUntil != null) {
				if (!attempts.blockedUntil.isAfter(now)) {
					iterator.remove();
				}
			} else if (!attempts.lastFailureAt.plus(FAILURE_WINDOW).isAfter(now)) {
				iterator.remove();
			}
		}
	}

	private void ensureCapacity() {
		if (failedAttempts.size() < MAX_TRACKED_KEYS) {
			return;
		}
		Iterator<String> iterator = failedAttempts.keySet().iterator();
		if (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	private String key(String email, String clientAddress) {
		return Objects.requireNonNull(email) + "\u0000" + Objects.requireNonNull(clientAddress);
	}

	private static final class FailedAttempts {

		private int count;
		private Instant lastFailureAt;
		private Instant blockedUntil;
	}
}
