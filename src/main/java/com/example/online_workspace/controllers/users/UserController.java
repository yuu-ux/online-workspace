package com.example.online_workspace.controllers.users;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.repositories.users.UserRepository.UserSummaryRow;
import com.example.online_workspace.services.UserSearchService;
import com.example.online_workspace.services.UserSearchService.Result;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserSearchService service;

	public UserController(UserSearchService service) {
		this.service = service;
	}

	@GetMapping
	public UserPageResponse search(
		@RequestParam("query") @NotBlank @Size(min = 1, max = 100) String query,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		Authentication authentication
	) {
		Result result = service.search(authentication.getName(), query, page, size);
		long totalPages = result.totalElements() / size
			+ (result.totalElements() % size == 0 ? 0 : 1);
		return new UserPageResponse(
			result.items().stream().map(UserSummaryResponse::from).toList(),
			new PageMetaResponse(
				page,
				size,
				result.totalElements(),
				totalPages,
				page == 0,
				totalPages == 0 || page >= totalPages - 1
			)
		);
	}

	public record UserPageResponse(List<UserSummaryResponse> items, PageMetaResponse page) {
	}

	public record UserSummaryResponse(long id, String name, String iconUrl) {
		private static UserSummaryResponse from(UserSummaryRow row) {
			return new UserSummaryResponse(row.id(), row.name(), row.iconUrl());
		}
	}

	public record PageMetaResponse(
		int page,
		int size,
		long totalElements,
		long totalPages,
		boolean first,
		boolean last
	) {
	}
}
