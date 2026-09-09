package com.example.online_workspace.configs.security;

import com.example.online_workspace.repositories.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * DB上で無効になったユーザーの既存セッションを継続させない。
 *
 * <p>Spring Securityの認証済みセッションは、認証成功後にユーザーDBを自動再確認しないため、
 * アカウント状態を変更できるアプリケーションではリクエストごとの再検証が必要になる。</p>
 */
final class ActiveUserAuthenticationFilter extends OncePerRequestFilter {

	private final UserRepository userRepository;
	private final SecurityContextRepository securityContextRepository;

	ActiveUserAuthenticationFilter(
		UserRepository userRepository,
		SecurityContextRepository securityContextRepository
	) {
		this.userRepository = userRepository;
		this.securityContextRepository = securityContextRepository;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (hasPersistedSecurityContext(request)
			&& isAuthenticatedUser(authentication)
			&& !userRepository.isActiveByEmail(authentication.getName())) {
			SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
			SecurityContextHolder.setContext(emptyContext);
			securityContextRepository.saveContext(emptyContext, request, response);
		}

		filterChain.doFilter(request, response);
	}

	private boolean hasPersistedSecurityContext(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session != null
			&& session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
				instanceof SecurityContext;
	}

	private boolean isAuthenticatedUser(Authentication authentication) {
		return authentication != null
			&& authentication.isAuthenticated()
			&& authentication.getPrincipal() instanceof UserDetails;
	}
}
