package com.example.online_workspace.controllers.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CsrfControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("CSRFトークン取得APIは未認証でも呼び出せる")
	@Test
	void csrfApiDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""))
			.andExpect(cookie().exists("XSRF-TOKEN"));
	}

	@DisplayName("有効なCSRFトークンとCookieを付与すれば状態変更リクエストは通過する")
	@Test
	void stateChangingApiAcceptsRequestWithValidCsrfToken() throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andExpect(cookie().exists("XSRF-TOKEN"))
			.andReturn();

		Cookie xsrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(xsrfCookie);

		mockMvc.perform(
				post("/api/v1/auth/csrf")
					.cookie(xsrfCookie)
					.header("X-CSRF-TOKEN", xsrfCookie.getValue()))
			.andExpect(status().isMethodNotAllowed());
	}

	@DisplayName("不正なCSRFトークンでは状態変更リクエストが403で拒否される")
	@Test
	void stateChangingApiRejectsRequestWithWrongCsrfToken() throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andExpect(cookie().exists("XSRF-TOKEN"))
			.andReturn();

		Cookie xsrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(xsrfCookie);

		mockMvc.perform(
				post("/api/v1/auth/csrf")
					.cookie(xsrfCookie)
					.header("X-CSRF-TOKEN", xsrfCookie.getValue() + "-wrong"))
			.andExpect(status().isForbidden());
	}

	@DisplayName("XSRF-TOKEN cookieの値をheaderに設定すれば状態変更リクエストは通過する")
	@Test
	void stateChangingApiAcceptsCookieTokenInHeader() throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andExpect(cookie().exists("XSRF-TOKEN"))
			.andReturn();

		Cookie xsrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(xsrfCookie);

		mockMvc.perform(
				post("/api/v1/auth/csrf")
					.cookie(xsrfCookie)
					.header("X-CSRF-TOKEN", xsrfCookie.getValue()))
			.andExpect(status().isMethodNotAllowed());
	}
}
