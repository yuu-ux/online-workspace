package com.example.online_workspace.configs.security;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.example.online_workspace.exceptions.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiRateLimitFilter extends OncePerRequestFilter {

	private final int requestsPerMinute;
	private final ApiErrorWriter apiErrorWriter;
	// ponytail: 単一インスタンス向け固定窓。複数台構成になったらRedis等の共有ストアへ置き換える。
	private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

	public ApiRateLimitFilter(
		int requestsPerMinute,
		ApiErrorWriter apiErrorWriter
	) {
		if (requestsPerMinute < 1) {
			throw new IllegalArgumentException("requests-per-minute must be at least 1");
		}
		this.requestsPerMinute = requestsPerMinute;
		this.apiErrorWriter = apiErrorWriter;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !ApiKeyAuthenticationFilter.isTargetRequest(request);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
			|| !authentication.isAuthenticated()
			|| authentication instanceof AnonymousAuthenticationToken) {
			filterChain.doFilter(request, response);
			return;
		}

		long minute = Instant.now().getEpochSecond() / 60;
		String key = request.getHeader(ApiKeyAuthenticationFilter.HEADER_NAME) == null
			? "session:" + authentication.getName()
			: "api-key";
		Window window = windows.compute(key, (ignored, current) ->
			current == null || current.minute() != minute
				? new Window(minute, 1)
				: new Window(minute, current.requests() + 1)
		);
		if (window.requests() <= requestsPerMinute) {
			filterChain.doFilter(request, response);
			return;
		}

		response.setHeader("Retry-After", Long.toString(60 - Instant.now().getEpochSecond() % 60));
		apiErrorWriter.write(
			request,
			response,
			HttpStatus.TOO_MANY_REQUESTS,
			"RATE_LIMIT_EXCEEDED",
			"リクエスト回数が上限を超えました。"
		);
	}

	private record Window(long minute, int requests) {
	}
}
