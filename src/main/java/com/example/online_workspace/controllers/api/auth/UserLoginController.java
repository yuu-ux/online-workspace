package com.example.online_workspace.controllers.api.auth;

import com.example.online_workspace.exceptions.InvalidLoginCredentialsException;
import com.example.online_workspace.exceptions.TooManyLoginAttemptsException;
import com.example.online_workspace.forms.auth.UserLoginForm;
import com.example.online_workspace.models.users.AuthenticatedUser;
import com.example.online_workspace.models.users.UserAuthentication;
import com.example.online_workspace.services.auth.LoginRateLimiter;
import com.example.online_workspace.services.auth.UserLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
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

	private final UserLoginService userLoginService;
	private final LoginRateLimiter loginRateLimiter;
	private final SecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

	public UserLoginController(
		UserLoginService userLoginService,
		LoginRateLimiter loginRateLimiter,
		SecurityContextRepository securityContextRepository,
		SessionAuthenticationStrategy sessionAuthenticationStrategy
	) {
		this.userLoginService = userLoginService;
		this.loginRateLimiter = loginRateLimiter;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
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
			throw new TooManyLoginAttemptsException(LoginRateLimiter.BLOCK_DURATION_SECONDS);
		}

		UserAuthentication user;
		try {
			user = userLoginService.authenticate(email, form.password());
		} catch (InvalidLoginCredentialsException exception) {
			loginRateLimiter.recordFailure(email, clientAddress);
			throw exception;
		}

		loginRateLimiter.reset(email, clientAddress);
		AuthenticatedUser authenticatedUser = user.toAuthenticatedUser();
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
			authenticatedUser,
			null,
			List.of()
		);
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		return authenticatedUser;
	}

	private String clientAddress(HttpServletRequest request) {
		String forwardedAddress = request.getHeader("X-Real-IP");
		return forwardedAddress == null || forwardedAddress.isBlank()
			? request.getRemoteAddr()
			: forwardedAddress;
	}
}
