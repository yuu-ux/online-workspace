package com.example.online_workspace.controllers.users;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.services.UserService;
import com.example.online_workspace.services.UserService.Result;
import com.example.online_workspace.services.UserService.UserProfile;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService service;

	public UserController(UserService service) {
		this.service = service;
	}

	@GetMapping
	public UserPageResponse search(
		@RequestParam @NotBlank @Size(max = 100) String query,
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

	@GetMapping("/{userId}")
	public Object getUser(@PathVariable @Positive long userId, Authentication authentication) {
		UserProfile profile = service.getUser(authentication.getName(), userId);
		if (profile.isPublic()) {
			return new PublicUserProfileResponse(
				profile.id(),
				profile.name(),
				profile.iconUrl(),
				true,
				profile.bio(),
				profile.workCategory() == null
					? null
					: new RoomCategoryResponse(
						profile.workCategory().id(),
						profile.workCategory().name(),
						profile.workCategory().description(),
						profile.workCategory().sortOrder()
					),
				profile.friendship()
			);
		}
		return new HiddenUserProfileResponse(profile.name(), profile.iconUrl(), false);
	}

	public record UserPageResponse(List<UserSummaryResponse> items, PageMetaResponse page) {
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

	public record UserSummaryResponse(long id, String name, String iconUrl) {
		private static UserSummaryResponse from(UserService.UserSummary user) {
			return new UserSummaryResponse(user.id(), user.name(), user.iconUrl());
		}
	}

	public record PublicUserProfileResponse(
		long id,
		String name,
		String iconUrl,
		boolean isPublic,
		String bio,
		RoomCategoryResponse workCategory,
		String friendship
	) {
	}

	public record HiddenUserProfileResponse(String name, String iconUrl, boolean isPublic) {
	}

	public record RoomCategoryResponse(long id, String name, String description, int sortOrder) {
	}
}
