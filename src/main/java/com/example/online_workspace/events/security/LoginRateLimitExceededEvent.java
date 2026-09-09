package com.example.online_workspace.events.security;

/**
 * ログイン試行がレート制限により拒否されたことを示すイベント。
 *
 * @param retryAfterSeconds 再試行可能になるまでの秒数
 */
public record LoginRateLimitExceededEvent(long retryAfterSeconds) {
}
