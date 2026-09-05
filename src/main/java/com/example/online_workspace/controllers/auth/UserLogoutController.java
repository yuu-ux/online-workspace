package com.example.online_workspace.controllers.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * サーバー側セッションを破棄するログアウトAPI。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UserLogoutController {

	private final LogoutHandler logoutHandler = new CompositeLogoutHandler(
		new SecurityContextLogoutHandler(),
		new CookieClearingLogoutHandler("JSESSIONID")
	);
	private final ApplicationEventPublisher applicationEventPublisher;

	public UserLogoutController(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) {
		logoutHandler.logout(request, response, authentication);
		applicationEventPublisher.publishEvent(new LogoutSuccessEvent(authentication));
	}
}
