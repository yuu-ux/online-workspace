package com.example.online_workspace.controllers.rooms;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.repositories.RoomRepository.RoomView;
import com.example.online_workspace.services.RoomService;
import com.example.online_workspace.services.RoomService.CreateRoomCommand;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

	private final RoomService service;

	public RoomController(RoomService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RoomDetailResponse create(
		@Valid @RequestBody CreateRoomRequest request,
		Authentication authentication
	) {
		return RoomDetailResponse.from(service.create(authentication.getName(), request.toCommand()));
	}

	public record CreateRoomRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull @Size(max = 500) String description,
		@NotNull @Positive Long categoryId,
		@NotNull WorkStyle workStyle,
		@NotNull @Min(2) @Max(12) Integer maxMembers,
		@NotNull RoomVisibility visibility
	) {
		CreateRoomCommand toCommand() {
			return new CreateRoomCommand(
				name,
				description,
				categoryId,
				workStyle.name(),
				maxMembers,
				visibility.name()
			);
		}
	}

	public enum WorkStyle {
		FOCUS,
		CHAT_OK
	}

	public enum RoomVisibility {
		PUBLIC,
		FRIENDS_ONLY
	}

	public record RoomDetailResponse(
		long id,
		String name,
		String description,
		RoomCategoryResponse category,
		String workStyle,
		int maxMembers,
		int currentMembers,
		String visibility,
		String status,
		UserSummaryResponse createdBy,
		boolean joinable,
		String joinRestriction,
		boolean member,
		Instant createdAt,
		Instant updatedAt
	) {
		static RoomDetailResponse from(RoomView room) {
			return new RoomDetailResponse(
				room.id(),
				room.name(),
				room.description(),
				new RoomCategoryResponse(
					room.categoryId(),
					room.categoryName(),
					room.categoryDescription(),
					room.categorySortOrder()
				),
				room.workStyle(),
				room.maxMembers(),
				1,
				room.visibility(),
				room.status(),
				new UserSummaryResponse(room.creatorId(), room.creatorName(), room.creatorIconUrl()),
				false,
				null,
				true,
				room.createdAt(),
				room.updatedAt()
			);
		}
	}

	public record RoomCategoryResponse(long id, String name, String description, int sortOrder) {
	}

	public record UserSummaryResponse(long id, String name, String iconUrl) {
	}
}
