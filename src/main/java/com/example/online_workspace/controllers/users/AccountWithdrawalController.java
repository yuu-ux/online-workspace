package com.example.online_workspace.controllers.users;

import com.example.online_workspace.forms.WithdrawRequest;
import com.example.online_workspace.services.AccountWithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class AccountWithdrawalController {

	private final AccountWithdrawalService withdrawalService;
	private final SessionRegistry sessionRegistry;

	public AccountWithdrawalController(AccountWithdrawalService withdrawalService, SessionRegistry sessionRegistry) {
		this.withdrawalService = withdrawalService;
		this.sessionRegistry = sessionRegistry;
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> withdrawCurrentUser(
		@Valid @RequestBody WithdrawRequest request,
		Authentication authentication,
		HttpServletRequest servletRequest
	) {
		withdrawalService.withdraw(authentication.getName(), request.password());
		sessionRegistry.getAllSessions(authentication.getPrincipal(), false)
			.forEach(SessionInformation::expireNow);

		HttpSession session = servletRequest.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return ResponseEntity.noContent().build();
	}
}
