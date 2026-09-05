package com.example.online_workspace.configs.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
final class SecurityAuditLogger {

	static final String LOGGER_NAME = "SECURITY_AUDIT";
	private static final Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

	@EventListener
	void authenticationSucceeded(AuthenticationSuccessEvent event) {
		LOGGER.info(
			"security_audit event=authentication outcome=success mechanism={}",
			event.getAuthentication().getClass().getSimpleName()
		);
	}

	@EventListener
	void authenticationFailed(AbstractAuthenticationFailureEvent event) {
		LOGGER.info(
			"security_audit event=authentication outcome=denied reason={}",
			event.getException().getClass().getSimpleName()
		);
	}

	@EventListener
	void logoutSucceeded(LogoutSuccessEvent event) {
		LOGGER.info("security_audit event=logout outcome=success");
	}

	static void authorizationDenied(String target, String method, int status) {
		LOGGER.info(
			"security_audit event=authorization outcome=denied target={} method={} status={}",
			target,
			method,
			status
		);
	}
}
