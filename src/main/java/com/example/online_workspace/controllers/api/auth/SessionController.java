package com.example.online_workspace.controllers.api.auth;

import com.example.online_workspace.models.users.AuthenticatedUser;
import com.example.online_workspace.repositories.users.UserRepository;
import com.example.online_workspace.services.auth.EmailNormalizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 現在のブラウザセッションに関するREST API。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class SessionController {
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final UserRepository userRepository;

	public SessionController(
		AuthenticationManager authenticationManager,
		SecurityContextRepository securityContextRepository,
		UserRepository userRepository
	) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.userRepository = userRepository;
	}

	@PostMapping("/login")
	public AuthenticatedUser login(
		@Valid @RequestBody LoginRequest request,
		HttpServletRequest httpRequest,
		HttpServletResponse httpResponse
	) {
		String email = EmailNormalizer.normalize(request.email());
		Authentication authentication = authenticationManager.authenticate(
			UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
		);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);
		return userRepository.findAuthenticatedByEmail(email).orElseThrow();
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		SecurityContextHolder.clearContext();
		securityContextRepository.saveContext(
			SecurityContextHolder.createEmptyContext(),
			request,
			response
		);
		if (request.getSession(false) != null) {
			request.getSession(false).invalidate();
		}
	}

	/**
	 * 現在のリクエストが認証済みセッションを持つかどうかを返す。
	 *
	 * @return 現在のセッション状態と認証済みユーザー
	 */
	@GetMapping("/session")
	public SessionStatusResponse getSessionStatus() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean authenticated = authentication != null
			&& authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken);

		AuthenticatedUser user = authenticated
			? userRepository.findAuthenticatedByEmail(authentication.getName()).orElse(null)
			: null;
		return new SessionStatusResponse(authenticated && user != null, user);
	}

	public record LoginRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(max = 72) String password
	) {
	}
}
