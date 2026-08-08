package com.example.online_workspace.controllers.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
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

	@Test
	void csrfApiDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
			.andExpect(jsonPath("$.parameterName").value("_csrf"))
			.andExpect(jsonPath("$.token").isString());
	}

	@Test
	void stateChangingApiAcceptsRequestWithValidCsrfToken() throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isString())
			.andReturn();

		String csrfToken = JsonPath.read(csrfResponse.getResponse().getContentAsString(), "$.token");
		Cookie xsrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");

		mockMvc.perform(
				post("/api/v1/auth/login")
					.cookie(xsrfCookie)
					.header("X-CSRF-TOKEN", csrfToken))
			.andExpect(status().isNotForbidden());
	}
}
