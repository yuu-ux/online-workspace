package com.example.online_workspace.controllers.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
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
 * React向けのログアウトAPI。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UserLogoutController {

	private final LogoutHandler logoutHandler = new CompositeLogoutHandler(
		new SecurityContextLogoutHandler(),
		new CookieClearingLogoutHandler("JSESSIONID")
	);

	/**
	 * セッションを無効化してログアウトする。
	 *
	 * @param request HTTPリクエスト
	 * @param response HTTPレスポンス
	 * @param authentication 現在の認証情報
	 */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) {
		logoutHandler.logout(request, response, authentication);
	}
}
