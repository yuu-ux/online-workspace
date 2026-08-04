package com.example.online_workspace.exceptions;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiErrorResponse> handleApiException(
		ApiException exception,
		HttpServletRequest request
	) {
		return problem(
			exception.getStatus(),
			ApiErrorResponseFactory.create(
				request,
				exception.getStatus(),
				exception.getCode(),
				exception.getMessage()
			)
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ValidationApiErrorResponse> handleMethodArgumentNotValid(
		MethodArgumentNotValidException exception,
		HttpServletRequest request
	) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
			.map(this::toFieldErrorResponse)
			.toList();
		return validationProblem(request, fieldErrors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ValidationApiErrorResponse> handleHandlerMethodValidation(
		HandlerMethodValidationException exception,
		HttpServletRequest request
	) {
		return validationProblem(
			request,
			List.of(new FieldErrorResponse("request", "INVALID", "入力値が不正です。"))
		);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ValidationApiErrorResponse> handleConstraintViolation(
		ConstraintViolationException exception,
		HttpServletRequest request
	) {
		List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations().stream()
			.map(violation -> new FieldErrorResponse(
				violation.getPropertyPath().toString(),
				"INVALID",
				violation.getMessage()
			))
			.toList();
		return validationProblem(request, fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
		HttpMessageNotReadableException exception,
		HttpServletRequest request
	) {
		return apiProblem(
			request,
			HttpStatus.BAD_REQUEST,
			"MALFORMED_JSON",
			"リクエストボディのJSON形式が不正です。"
		);
	}

	@ExceptionHandler({ServletRequestBindingException.class, MethodArgumentTypeMismatchException.class})
	ResponseEntity<ApiErrorResponse> handleInvalidParameter(Exception exception, HttpServletRequest request) {
		return apiProblem(
			request,
			HttpStatus.BAD_REQUEST,
			"INVALID_PARAMETER",
			"リクエストパラメーターが不正です。"
		);
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ApiErrorResponse> handleResponseStatus(
		ResponseStatusException exception,
		HttpServletRequest request
	) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
		if (!status.is4xxClientError()) {
			return internalServerError(exception, request);
		}

		return apiProblem(
			request,
			status,
			statusCode(status),
			exception.getReason() == null ? statusMessage(status) : exception.getReason()
		);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNoResourceFound(
		NoResourceFoundException exception,
		HttpServletRequest request
	) {
		return apiProblem(
			request,
			HttpStatus.NOT_FOUND,
			"RESOURCE_NOT_FOUND",
			"対象のリソースが見つかりません。"
		);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
		HttpRequestMethodNotSupportedException exception,
		HttpServletRequest request
	) {
		return apiProblem(
			request,
			HttpStatus.METHOD_NOT_ALLOWED,
			"METHOD_NOT_ALLOWED",
			"このHTTPメソッドは利用できません。"
		);
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiErrorResponse> handleAuthentication(
		AuthenticationException exception,
		HttpServletRequest request
	) {
		return apiProblem(
			request,
			HttpStatus.UNAUTHORIZED,
			"UNAUTHORIZED",
			"認証が必要です。"
		);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiErrorResponse> handleAccessDenied(
		AccessDeniedException exception,
		HttpServletRequest request
	) {
		return apiProblem(
			request,
			HttpStatus.FORBIDDEN,
			"FORBIDDEN",
			"この操作を行う権限がありません。"
		);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpectedException(
		Exception exception,
		HttpServletRequest request
	) {
		return internalServerError(exception, request);
	}

	private ResponseEntity<ApiErrorResponse> internalServerError(
		Exception exception,
		HttpServletRequest request
	) {
		ApiErrorResponse response = ApiErrorResponseFactory.create(
			request,
			HttpStatus.INTERNAL_SERVER_ERROR,
			"INTERNAL_SERVER_ERROR",
			"予期しないエラーが発生しました。"
		);
		log.error("Unexpected API error traceId={} path={}", response.traceId(), response.path(), exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, response);
	}

	private String statusCode(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> "BAD_REQUEST";
			case UNAUTHORIZED -> "UNAUTHORIZED";
			case FORBIDDEN -> "FORBIDDEN";
			case NOT_FOUND -> "RESOURCE_NOT_FOUND";
			case CONFLICT -> "CONFLICT";
			case UNPROCESSABLE_CONTENT -> "VALIDATION_FAILED";
			case TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS";
			default -> status.name();
		};
	}

	private String statusMessage(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> "リクエストが不正です。";
			case UNAUTHORIZED -> "認証が必要です。";
			case FORBIDDEN -> "この操作を行う権限がありません。";
			case NOT_FOUND -> "対象のリソースが見つかりません。";
			case CONFLICT -> "現在の状態では操作できません。";
			case UNPROCESSABLE_CONTENT -> "入力内容を確認してください。";
			case TOO_MANY_REQUESTS -> "リクエスト回数が上限を超えました。";
			default -> "リクエストを処理できませんでした。";
		};
	}

	private FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
		return new FieldErrorResponse(
			fieldError.getField(),
			validationCode(fieldError.getCode()),
			fieldError.getDefaultMessage() == null ? "入力値が不正です。" : fieldError.getDefaultMessage()
		);
	}

	private String validationCode(String springValidationCode) {
		if (springValidationCode == null) {
			return "INVALID";
		}
		return switch (springValidationCode) {
			case "NotBlank", "NotEmpty", "NotNull" -> "REQUIRED";
			case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero",
				"Negative", "NegativeOrZero" -> "RANGE";
			case "Size" -> "SIZE";
			case "Email" -> "FORMAT";
			case "Pattern" -> "PATTERN";
			default -> "INVALID";
		};
	}

	private ResponseEntity<ApiErrorResponse> apiProblem(
		HttpServletRequest request,
		HttpStatus status,
		String code,
		String message
	) {
		return problem(status, ApiErrorResponseFactory.create(request, status, code, message));
	}

	private ResponseEntity<ValidationApiErrorResponse> validationProblem(
		HttpServletRequest request,
		List<FieldErrorResponse> fieldErrors
	) {
		return problem(
			HttpStatus.UNPROCESSABLE_CONTENT,
			ApiErrorResponseFactory.createValidation(request, fieldErrors)
		);
	}

	private <T> ResponseEntity<T> problem(HttpStatus status, T response) {
		return ResponseEntity.status(status)
			.contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.body(response);
	}
}
