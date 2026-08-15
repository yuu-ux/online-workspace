package com.example.online_workspace.controllers.api.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 現在のブラウザセッションに関するREST API。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class SessionController {

	/**
	 * 現在のリクエストが認証済みセッションを持つかどうかを返す。
	 *
	 * @return 現在のセッション状態。ユーザー情報は認証機能の実装時に追加する
	 */
	@GetMapping("/session")
	public SessionStatusResponse getSessionStatus() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean authenticated = authentication != null
			&& authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken);

		return new SessionStatusResponse(authenticated, null);
	}
}
