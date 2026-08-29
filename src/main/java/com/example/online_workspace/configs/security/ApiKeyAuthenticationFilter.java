package com.example.online_workspace.configs.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.example.online_workspace.exceptions.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	static final String HEADER_NAME = "X-API-Key";
	private static final Pattern TARGET_PATH = Pattern.compile("^/api/v1/rooms(?:/[^/]+)?/?$");
	private static final Set<String> TARGET_METHODS = Set.of("GET", "POST", "PUT", "DELETE");

	private final byte[] expectedApiKey;
	private final String principal;
	private final ApiErrorWriter apiErrorWriter;

	public ApiKeyAuthenticationFilter(
		String apiKey,
		String principal,
		ApiErrorWriter apiErrorWriter
	) {
		this.expectedApiKey = apiKey.getBytes(StandardCharsets.UTF_8);
		this.principal = principal;
		this.apiErrorWriter = apiErrorWriter;
	}

	boolean hasValidApiKey(HttpServletRequest request) {
		String apiKey = request.getHeader(HEADER_NAME);
		return isTargetRequest(request)
			&& apiKey != null
			&& expectedApiKey.length > 0
			&& !principal.isBlank()
			&& MessageDigest.isEqual(expectedApiKey, apiKey.getBytes(StandardCharsets.UTF_8));
	}

	static boolean isTargetRequest(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return TARGET_METHODS.contains(request.getMethod()) && TARGET_PATH.matcher(path).matches();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !isTargetRequest(request);
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (request.getHeader(HEADER_NAME) == null) {
			filterChain.doFilter(request, response);
			return;
		}
		if (!hasValidApiKey(request)) {
			apiErrorWriter.write(
				request,
				response,
				HttpStatus.UNAUTHORIZED,
				"INVALID_API_KEY",
				"API keyが無効です。"
			);
			return;
		}

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		));
		SecurityContextHolder.setContext(context);
		filterChain.doFilter(request, response);
	}
}
