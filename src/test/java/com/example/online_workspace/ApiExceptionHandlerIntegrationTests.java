package com.example.online_workspace;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.online_workspace.exceptions.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiExceptionHandlerIntegrationTests.ErrorEndpoint.class)
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
	void requestParameterValidationUsesBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/parameter-validation")
				.with(user("tester"))
				.param("page", "-1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("page"))
			.andExpect(jsonPath("$.fieldErrors[0].code").value("RANGE"))
			.andExpect(jsonPath("$.fieldErrors[0].message").isString());
	}

	@Test
	void frameworkClientErrorsPreserveTheirStatus() throws Exception {
		mockMvc.perform(post("/api/v1/test/errors/validation")
				.with(user("tester"))
				.with(csrf())
				.contentType(MediaType.TEXT_PLAIN)
				.content("name=test"))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.status").value(415))
			.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

		mockMvc.perform(get("/api/v1/test/errors/json")
				.with(user("tester"))
				.accept(MediaType.TEXT_PLAIN))
			.andExpect(status().isNotAcceptable())
			.andExpect(jsonPath("$.status").value(406))
			.andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
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
	void controllerAuthenticationExceptionsPreserveUnauthorizedStatus() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/authentication").with(user("tester")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void controllerAccessDeniedExceptionsPreserveForbiddenStatus() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/access-denied").with(user("tester")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void responseStatusExceptionsPreserveDeclaredStatus() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/status").with(user("tester")))
			.andExpect(status().isGone())
			.andExpect(jsonPath("$.status").value(410))
			.andExpect(jsonPath("$.message").value("このリソースは廃止されました。"));
	}

	@Test
	void responseStatusExceptionsHandleNonStandardAndSanitizeServerErrors() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/non-standard-status").with(user("tester")))
			.andExpect(status().is(499))
			.andExpect(jsonPath("$.status").value(499))
			.andExpect(jsonPath("$.code").value("HTTP_499"));

		mockMvc.perform(get("/api/v1/test/errors/server-status").with(user("tester")))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value("予期しないエラーが発生しました。"));
	}

	@Test
	void methodNotAllowedPreservesAllowHeader() throws Exception {
		mockMvc.perform(get("/api/v1/test/errors/validation").with(user("tester")))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(header().string(HttpHeaders.ALLOW, "POST"));
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
			.andExpect(jsonPath("$.traceId", matchesPattern(TRACE_ID_PATTERN)));
	}

	@RestController
	@RequestMapping("/api/v1/test/errors")
	static class ErrorEndpoint {

		@PostMapping("/validation")
		void validate(@Valid @RequestBody ValidationRequest request) {
		}

		@GetMapping("/parameter-validation")
		void validateParameter(@RequestParam @Min(0) int page) {
		}

		@GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
		String json() {
			return "{}";
		}

		@GetMapping("/authentication")
		void authenticationError() {
			throw new BadCredentialsException("invalid credentials");
		}

		@GetMapping("/access-denied")
		void accessDeniedError() {
			throw new AccessDeniedException("forbidden");
		}

		@GetMapping("/status")
		void responseStatusError() {
			throw new ResponseStatusException(HttpStatus.GONE, "このリソースは廃止されました。");
		}

		@GetMapping("/non-standard-status")
		void nonStandardStatusError() {
			throw new ResponseStatusException(499, null, null);
		}

		@GetMapping("/server-status")
		void serverStatusError() {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "secret implementation detail");
		}

		@GetMapping("/not-found")
		void notFound() {
			throw new ApiException(
				HttpStatus.NOT_FOUND,
				"TEST_RESOURCE_NOT_FOUND",
				"テスト用リソースが見つかりません。"
			);
		}

		@GetMapping("/business")
		void businessError() {
			throw new ApiException(HttpStatus.CONFLICT, "TEST_STATE_CONFLICT", "現在の状態では操作できません。");
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
