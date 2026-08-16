package com.example.online_workspace.controllers.api.rooms;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.models.RoomListItem;
import com.example.online_workspace.services.RoomListService;
import com.example.online_workspace.services.RoomListService.Result;

@Validated
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomListController {

	private final RoomListService service;

	public RoomListController(RoomListService service) {
		this.service = service;
	}

	@GetMapping
	public RoomPageResponse list(
		@RequestParam(required = false) @Positive Long categoryId,
		@RequestParam(required = false) WorkStyle workStyle,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		Authentication authentication
	) {
		Result result = service.list(
			authentication.getName(),
			categoryId,
			workStyle == null ? null : workStyle.name(),
			page,
			size
		);
		int totalPages = (int) (result.totalElements() / size
			+ (result.totalElements() % size == 0 ? 0 : 1));
		return new RoomPageResponse(
			result.items().stream().map(RoomSummaryResponse::from).toList(),
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

	public enum WorkStyle {
		FOCUS,
		CHAT_OK
	}

	public record RoomPageResponse(List<RoomSummaryResponse> items, PageMetaResponse page) {
	}

	public record PageMetaResponse(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
	) {
	}

	public record RoomSummaryResponse(
		long id,
		String name,
		RoomCategoryResponse category,
		String workStyle,
		int maxMembers,
		int currentMembers,
		String visibility,
		String status,
		UserSummaryResponse createdBy,
		boolean joinable,
		String joinRestriction,
		Instant createdAt
	) {
		private static RoomSummaryResponse from(RoomListItem item) {
			String restriction = item.blocked()
				? "BLOCKED"
				: item.currentMembers() >= item.maxMembers() ? "FULL" : null;
			return new RoomSummaryResponse(
				item.id(),
				item.name(),
				new RoomCategoryResponse(
					item.categoryId(),
					item.categoryName(),
					item.categoryDescription(),
					item.categorySortOrder()
				),
				item.workStyle(),
				item.maxMembers(),
				item.currentMembers(),
				item.visibility(),
				item.status(),
				new UserSummaryResponse(item.creatorId(), item.creatorName(), item.creatorIconUrl()),
				restriction == null,
				restriction,
				item.createdAt()
			);
		}
	}

	public record RoomCategoryResponse(long id, String name, String description, int sortOrder) {
	}

	public record UserSummaryResponse(long id, String name, String iconUrl) {
	}
}
