package com.example.online_workspace.controllers.auth;

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
import org.springframework.security.authentication.AuthenticationEventPublisher;
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
 * セッション認証を開始するログインAPI。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UserLoginController {

	private final UserLoginService userLoginService;
	private final LoginRateLimiter loginRateLimiter;
	private final SecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final AuthenticationEventPublisher authenticationEventPublisher;

	public UserLoginController(
		UserLoginService userLoginService,
		LoginRateLimiter loginRateLimiter,
		SecurityContextRepository securityContextRepository,
		SessionAuthenticationStrategy sessionAuthenticationStrategy,
		AuthenticationEventPublisher authenticationEventPublisher
	) {
		this.userLoginService = userLoginService;
		this.loginRateLimiter = loginRateLimiter;
		this.securityContextRepository = securityContextRepository;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.authenticationEventPublisher = authenticationEventPublisher;
	}

	@PostMapping("/login")
	public AuthenticatedUser login(
		@Valid @RequestBody UserLoginForm form,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		String clientAddress = request.getRemoteAddr();
		if (loginRateLimiter.isBlocked(form.email(), clientAddress)) {
			throw new TooManyLoginAttemptsException(LoginRateLimiter.RETRY_AFTER_SECONDS);
		}

		Authentication attemptedAuthentication = UsernamePasswordAuthenticationToken.unauthenticated(form.email(), null);
		UserAuthentication user;
		try {
			user = userLoginService.authenticate(form.email(), form.password());
		} catch (InvalidLoginCredentialsException exception) {
			loginRateLimiter.recordFailure(form.email(), clientAddress);
			authenticationEventPublisher.publishAuthenticationFailure(
				new BadCredentialsException("Invalid login credentials"),
				attemptedAuthentication
			);
			throw exception;
		}

		loginRateLimiter.reset(form.email(), clientAddress);
		AuthenticatedUser responseUser = user.toAuthenticatedUser();
		UsernamePasswordAuthenticationToken authentication =
			UsernamePasswordAuthenticationToken.authenticated(user.email(), null, List.of());
		authentication.setDetails(responseUser);
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		authenticationEventPublisher.publishAuthenticationSuccess(authentication);
		return responseUser;
	}
}
