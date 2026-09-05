package com.example.online_workspace.configs.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

final class SecurityAuditFilter extends OncePerRequestFilter {

	private final String target;

	SecurityAuditFilter(String target) {
		this.target = target;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		filterChain.doFilter(request, response);
		if (response.getStatus() == 401 || response.getStatus() == 403) {
			SecurityAuditLogger.authorizationDenied(target, request.getMethod(), response.getStatus());
		}
	}
}
