package com.example.online_workspace;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.online_workspace.exceptions.BusinessException;
import com.example.online_workspace.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiExceptionHandlerIntegrationTests.ErrorEndpointConfiguration.class)
class ApiExceptionHandlerIntegrationTests {

	private static final String TRACE_ID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void validationErrorsUseTheOpenApiErrorSchema() throws Exception {
		mockMvc.perform(post("/api/v1/test/errors/validation")
				.with(user("tester"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\"}"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(422))
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.path").value("/api/v1/test/errors/validation"))
			.andExpect(jsonPath("$.timestamp").isString())
			.andExpect(jsonPath("$.traceId", matchesPattern(TRACE_ID_PATTERN)))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
			.andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("名前は必須です。"));
	}

	@Test
	void malformedJsonUsesTheCommonBadRequestFormat() throws Exception {
		mockMvc.perform(post("/api/v1/test/errors/validation")
				.with(user("tester"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
			.andExpect(jsonPath("$.message").value("リクエストボディのJSON形式が不正です。"));
	}

	@Test
	void authenticationErrorsUseTheCommonJsonFormat() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/not-found"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.message").value("認証が必要です。"))
			.andExpect(jsonPath("$.traceId", matchesPattern(TRACE_ID_PATTERN)));
	}

	@Test
	void authorizationErrorsUseTheCommonJsonFormat() throws Exception {
		mockMvc.perform(post("/api/v1/test/errors/validation")
				.with(user("tester"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"valid\"}"))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.message").value("この操作を行う権限がありません。"));
	}

	@Test
	void resourceNotFoundErrorsUseTheCommonJsonFormat() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/not-found").with(user("tester")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("TEST_RESOURCE_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("テスト用リソースが見つかりません。"));
	}

	@Test
	void unmappedApiPathsUseTheCommonNotFoundFormat() throws Exception {
		mockMvc.perform(get("/api/v1/does-not-exist").with(user("tester")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
			.andExpect(jsonPath("$.path").value("/api/v1/does-not-exist"));
	}

	@Test
	void businessErrorsUseConflictAndTheCommonJsonFormat() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/business").with(user("tester")))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.code").value("TEST_STATE_CONFLICT"))
			.andExpect(jsonPath("$.message").value("現在の状態では操作できません。"));
	}

	@Test
	void unexpectedErrorsReturn500WithoutLeakingInternalDetails() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/unexpected").with(user("tester")))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value("予期しないエラーが発生しました。"))
			.andExpect(jsonPath("$.message").value(not(containsString("secret"))))
			.andExpect(jsonPath("$.traceId", matchesPattern(TRACE_ID_PATTERN)));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ErrorEndpointConfiguration {

		@Bean
		ErrorEndpoint errorEndpoint() {
			return new ErrorEndpoint();
		}
	}

	@RestController
	@RequestMapping("/api/v1/test/errors")
	static class ErrorEndpoint {

		@PostMapping("/validation")
		void validate(@Valid @RequestBody ValidationRequest request) {
		}

		@GetMapping("/not-found")
		void notFound() {
			throw new ResourceNotFoundException(
				"TEST_RESOURCE_NOT_FOUND",
				"テスト用リソースが見つかりません。"
			);
		}

		@GetMapping("/business")
		void businessError() {
			throw new BusinessException("TEST_STATE_CONFLICT", "現在の状態では操作できません。");
		}

		@GetMapping("/unexpected")
		void unexpectedError() {
			throw new IllegalStateException("secret implementation detail");
		}
	}

	record ValidationRequest(
		@NotBlank(message = "名前は必須です。") String name
	) {
	}
}
