package com.example.online_workspace.controllers.auth;

import com.example.online_workspace.events.security.LoginRateLimitExceededEvent;
import com.example.online_workspace.exceptions.InvalidLoginCredentialsException;
import com.example.online_workspace.exceptions.TooManyLoginAttemptsException;
import com.example.online_workspace.forms.auth.UserLoginForm;
import com.example.online_workspace.models.users.AuthenticatedUser;
import com.example.online_workspace.repositories.users.UserRepository;
import com.example.online_workspace.repositories.users.UserRepository.MyProfileRow;
import com.example.online_workspace.services.auth.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * React向けのログインAPI。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UserLoginController {

	private final AuthenticationManager authenticationManager;
	private final LoginRateLimiter loginRateLimiter;
	private final UserRepository userRepository;
	private final SecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final ApplicationEventPublisher applicationEventPublisher;

	public UserLoginController(
		AuthenticationManager authenticationManager,
		LoginRateLimiter loginRateLimiter,
		UserRepository userRepository,
		SecurityContextRepository securityContextRepository,
		SessionAuthenticationStrategy sessionAuthenticationStrategy,
		ApplicationEventPublisher applicationEventPublisher
	) {
		this.authenticationManager = authenticationManager;
		this.loginRateLimiter = loginRateLimiter;
		this.userRepository = userRepository;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * メールアドレスとパスワードでログインする。
	 *
	 * @param form ログイン入力値
	 * @param request HTTPリクエスト
	 * @param response HTTPレスポンス
	 * @return 認証済みユーザー情報
	 */
	@PostMapping("/login")
	public AuthenticatedUser login(
		@Valid @RequestBody UserLoginForm form,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		String email = form.email();
		String clientAddress = clientAddress(request);
		if (loginRateLimiter.isBlocked(email, clientAddress)) {
			applicationEventPublisher.publishEvent(
				new LoginRateLimitExceededEvent(LoginRateLimiter.BLOCK_DURATION_SECONDS)
			);
			throw new TooManyLoginAttemptsException(LoginRateLimiter.BLOCK_DURATION_SECONDS);
		}

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(email, form.password())
			);
		} catch (BadCredentialsException exception) {
			loginRateLimiter.recordFailure(email, clientAddress);
			throw new InvalidLoginCredentialsException();
		}

		loginRateLimiter.reset(email, clientAddress);
		MyProfileRow user = userRepository.findMyProfileByEmail(authentication.getName());
		if (user == null) {
			throw new InvalidLoginCredentialsException();
		}
		AuthenticatedUser authenticatedUser = user.toAuthenticatedUser();
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		return authenticatedUser;
	}

	private String clientAddress(HttpServletRequest request) {
		return request.getRemoteAddr();
	}
}
