package com.example.online_workspace.exceptions;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
		return validationProblem(request, HttpStatus.UNPROCESSABLE_CONTENT, fieldErrors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ValidationApiErrorResponse> handleHandlerMethodValidation(
		HandlerMethodValidationException exception,
		HttpServletRequest request
	) {
		List<FieldErrorResponse> fieldErrors = exception.getParameterValidationResults().stream()
			.flatMap(result -> {
				String parameterName = result.getMethodParameter().getParameterName();
				return result.getResolvableErrors().stream()
					.map(error -> new FieldErrorResponse(
						parameterName == null ? "request" : parameterName,
						validationCode(error),
						error.getDefaultMessage() == null ? "入力値が不正です。" : error.getDefaultMessage()
					));
			})
			.toList();
		return validationProblem(request, HttpStatus.BAD_REQUEST, fieldErrors);
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
		return validationProblem(request, HttpStatus.UNPROCESSABLE_CONTENT, fieldErrors);
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
		return problem(
			HttpStatus.METHOD_NOT_ALLOWED,
			exception.getHeaders(),
			ApiErrorResponseFactory.create(
				request,
				HttpStatus.METHOD_NOT_ALLOWED,
				"METHOD_NOT_ALLOWED",
				"このHTTPメソッドは利用できません。"
			)
		);
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiErrorResponse> handleAuthentication(
		AuthenticationException exception,
		HttpServletRequest request
	) {
		return apiProblem(request, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiErrorResponse> handleAccessDenied(
		AccessDeniedException exception,
		HttpServletRequest request
	) {
		return apiProblem(request, HttpStatus.FORBIDDEN, "FORBIDDEN", "この操作を行う権限がありません。");
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ApiErrorResponse> handleResponseStatus(
		ResponseStatusException exception,
		HttpServletRequest request
	) {
		HttpStatusCode status = exception.getStatusCode();
		if (status.is5xxServerError()) {
			return internalServerError(exception, request);
		}
		HttpStatus resolved = HttpStatus.resolve(status.value());
		return problem(
			status,
			exception.getHeaders(),
			ApiErrorResponseFactory.create(
				request,
				status,
				resolved == null ? "HTTP_" + status.value() : resolved.name(),
				exception.getReason() == null
					? (resolved == null ? "リクエストを処理できません。" : resolved.getReasonPhrase())
					: exception.getReason()
			)
		);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpectedException(
		Exception exception,
		HttpServletRequest request
	) {
		if (exception instanceof ErrorResponse errorResponse) {
			HttpStatusCode status = errorResponse.getStatusCode();
			HttpStatus httpStatus = HttpStatus.resolve(status.value());
			return problem(
				status,
				errorResponse.getHeaders(),
				ApiErrorResponseFactory.create(
					request,
					status,
					httpStatus == null ? "HTTP_ERROR" : httpStatus.name(),
					errorResponse.getBody().getDetail()
				)
			);
		}
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
			case "Size", "MaxUtf8ByteLength" -> "SIZE";
			case "Email" -> "FORMAT";
			case "Pattern" -> "PATTERN";
			default -> "INVALID";
		};
	}

	private String validationCode(MessageSourceResolvable error) {
		String[] codes = error.getCodes();
		return validationCode(codes == null || codes.length == 0 ? null : codes[codes.length - 1]);
	}

	private ResponseEntity<ApiErrorResponse> apiProblem(
		HttpServletRequest request,
		HttpStatusCode status,
		String code,
		String message
	) {
		return problem(status, ApiErrorResponseFactory.create(request, status, code, message));
	}

	private ResponseEntity<ValidationApiErrorResponse> validationProblem(
		HttpServletRequest request,
		HttpStatus status,
		List<FieldErrorResponse> fieldErrors
	) {
		return problem(
			status,
			ApiErrorResponseFactory.createValidation(request, status, fieldErrors)
		);
	}

	private <T> ResponseEntity<T> problem(HttpStatusCode status, T response) {
		return problem(status, HttpHeaders.EMPTY, response);
	}

	private <T> ResponseEntity<T> problem(HttpStatusCode status, HttpHeaders headers, T response) {
		return ResponseEntity.status(status)
			.headers(headers)
			.contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.body(response);
	}
}
