package com.example.online_workspace.configs.security;

import com.example.online_workspace.models.users.AuthenticatedUserPrincipal;
import com.example.online_workspace.repositories.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 既存セッションのユーザーが現在も認証可能かを確認する。
 */
final class ActiveUserAuthenticationFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;

	ActiveUserAuthenticationFilter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (isApplicationSessionAuthentication(authentication)
			&& !userRepository.isActiveByEmail(authentication.getName())) {
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private boolean isApplicationSessionAuthentication(Authentication authentication) {
		return authentication != null
			&& authentication.isAuthenticated()
			&& authentication.getPrincipal() instanceof AuthenticatedUserPrincipal;
	}
}
