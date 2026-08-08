package com.example.online_workspace;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class SwaggerUiIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void swaggerUiIsAvailableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", containsString("/swagger-ui/")));

		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Swagger UI")));
	}

	@Test
	void swaggerUiLoadsTheVersionedOpenApiContract() throws Exception {
		mockMvc.perform(get("/openapi.yaml"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("title: Online Workspace API")))
			.andExpect(content().string(containsString("CreateRoomRequest:")));

		mockMvc.perform(get("/v3/api-docs/swagger-config"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.url").value("/openapi.yaml"));
	}

	@Test
	void generatedOpenApiEndpointIsDisabled() throws Exception {
		mockMvc.perform(get("/v3/api-docs").with(user("tester")))
			.andExpect(status().isNotFound());
	}

	@Test
	void unauthenticatedApiRequestDoesNotRedirectToLoginPage() throws Exception {
		mockMvc.perform(get("/api/v1/rooms"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().doesNotExist("Location"));
	}

	@Test
	void sessionApiDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/session"))
			.andExpect(status().isNotFound());
	}

	@Test
	void csrfApiDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
			.andExpect(jsonPath("$.parameterName").value("_csrf"))
			.andExpect(jsonPath("$.token").isString());
	}

	@Test
	void registerApiDoesNotRequireSessionAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").with(csrf()))
			.andExpect(status().isNotFound());
	}

	@Test
	void loginApiDoesNotRequireSessionAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").with(csrf()))
			.andExpect(status().isNotFound());
	}

	@Test
	void stateChangingApiRejectsRequestWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login"))
			.andExpect(status().isForbidden());
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
