package com.example.online_workspace.controllers.friends;

import java.time.Instant;
import java.util.List;

import com.example.online_workspace.services.FriendService;
import com.example.online_workspace.services.FriendService.FriendItem;
import com.example.online_workspace.services.FriendService.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

	private final FriendService service;

	public FriendController(FriendService service) {
		this.service = service;
	}

	@GetMapping
	public FriendPageResponse list(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		Authentication authentication
	) {
		Result result = service.list(authentication.getName(), page, size);
		long totalPages = result.totalElements() / size
			+ (result.totalElements() % size == 0 ? 0 : 1);
		return new FriendPageResponse(
			result.items().stream().map(FriendResponse::from).toList(),
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

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public FriendResponse add(
		@Valid @RequestBody CreateFriendRequest request,
		Authentication authentication
	) {
		return FriendResponse.from(service.add(authentication.getName(), request.userId()));
	}

	@DeleteMapping("/{friendUserId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long friendUserId, Authentication authentication) {
		service.delete(authentication.getName(), friendUserId);
	}

	public record CreateFriendRequest(@NotNull @Positive Long userId) {
	}

	public record FriendPageResponse(List<FriendResponse> items, PageMetaResponse page) {
	}

	public record FriendResponse(UserSummaryResponse user, boolean online, Instant createdAt) {
		private static FriendResponse from(FriendItem item) {
			return new FriendResponse(
				new UserSummaryResponse(item.id(), item.name(), item.iconUrl()),
				item.online(),
				item.createdAt()
			);
		}
	}

	public record UserSummaryResponse(long id, String name, String iconUrl) {
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
