package com.example.online_workspace.events.security;

/**
 * ログイン試行がレート制限により拒否されたことを通知する。
 *
 * @param retryAfterSeconds 再試行までの秒数
 */
public record LoginRateLimitExceededEvent(long retryAfterSeconds) {
}
