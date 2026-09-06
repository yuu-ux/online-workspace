package com.example.online_workspace.controllers.auth;

import com.example.online_workspace.models.users.AuthenticatedUser;
import com.example.online_workspace.models.users.AuthenticatedUserPrincipal;
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
	 * @return 現在のセッション状態。認証済みの場合はユーザー情報を含む
	 */
	@GetMapping("/session")
	public SessionStatusResponse getSessionStatus() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean authenticated = authentication != null
			&& authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken);

		AuthenticatedUser user = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal
			? principal.user()
			: null;
		return new SessionStatusResponse(authenticated, user);
	}
}
