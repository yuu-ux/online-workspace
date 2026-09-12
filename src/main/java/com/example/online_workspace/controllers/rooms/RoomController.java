package com.example.online_workspace.controllers.rooms;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.models.RoomListItem;
import com.example.online_workspace.repositories.RoomRepository.RoomView;
import com.example.online_workspace.services.RoomListService;
import com.example.online_workspace.services.RoomListService.Result;
import com.example.online_workspace.services.NotificationService;
import com.example.online_workspace.services.RoomService;
import com.example.online_workspace.services.RoomService.CreateRoomCommand;
import com.example.online_workspace.services.RoomService.UpdateRoomCommand;

@RestController
@RequestMapping({"/api/v1/rooms", "/api/v1/public/rooms"})
public class RoomController {

	private final RoomService service;
	private final RoomListService listService;
	private final SimpMessagingTemplate messagingTemplate;
	private final NotificationService notificationService;

	public RoomController(
		RoomService service,
		RoomListService listService,
		SimpMessagingTemplate messagingTemplate,
		NotificationService notificationService
	) {
		this.service = service;
		this.listService = listService;
		this.messagingTemplate = messagingTemplate;
		this.notificationService = notificationService;
	}

	@GetMapping
	public RoomPageResponse list(
		@RequestParam(required = false) @Positive Long categoryId,
		@RequestParam(required = false) WorkStyle workStyle,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		Authentication authentication
	) {
		Result result = listService.list(
			authentication.getName(),
			categoryId,
			workStyle == null ? null : workStyle.name(),
			page,
			size
		);
		long totalPages = result.totalElements() / size
			+ (result.totalElements() % size == 0 ? 0 : 1);
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

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RoomDetailResponse create(
		@Valid @RequestBody CreateRoomRequest request,
		Authentication authentication
	) {
		RoomDetailResponse response =
			RoomDetailResponse.from(service.create(authentication.getName(), request.toCommand()));
		messagingTemplate.convertAndSend(
			"/topic/rooms",
			new RoomCreatedEvent("room:created", response)
		);
		notificationService.publishTo(authentication.getName(), "ルームを作成しました。");
		return response;
	}

	@GetMapping("/{roomId}")
	public RoomDetailResponse get(@PathVariable @Positive long roomId, Authentication authentication) {
		return RoomDetailResponse.from(service.get(authentication.getName(), roomId));
	}

	@PutMapping("/{roomId}")
	public RoomDetailResponse update(
		@PathVariable @Positive long roomId,
		@Valid @RequestBody UpdateRoomRequest request,
		Authentication authentication
	) {
		RoomDetailResponse response =
			RoomDetailResponse.from(service.update(authentication.getName(), roomId, request.toCommand()));
		notificationService.publishTo(authentication.getName(), "ルームを更新しました。");
		return response;
	}

	@DeleteMapping("/{roomId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void close(@PathVariable @Positive long roomId, Authentication authentication) {
		service.close(authentication.getName(), roomId);
		notificationService.publishTo(authentication.getName(), "ルームを削除しました。");
	}

	public record CreateRoomRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull @Size(max = 500) String description,
		@NotNull @Positive Long categoryId,
		@NotNull WorkStyle workStyle,
		@NotNull @Min(2) @Max(12) Integer maxMembers
	) {
		private CreateRoomCommand toCommand() {
			return new CreateRoomCommand(
				name,
				description,
				categoryId,
				workStyle.name(),
				maxMembers
			);
		}
	}

	public record UpdateRoomRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull @Size(max = 500) String description,
		@NotNull @Positive Long categoryId,
		@NotNull WorkStyle workStyle,
		@NotNull @Min(2) @Max(12) Integer maxMembers
	) {
		private UpdateRoomCommand toCommand() {
			return new UpdateRoomCommand(
				name,
				description,
				categoryId,
				workStyle.name(),
				maxMembers
			);
		}
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
		long totalPages,
		boolean first,
		boolean last
	) {
	}

	public record RoomSummaryResponse(
		long id,
		String name,
		String description,
		RoomCategoryResponse category,
		String workStyle,
		int maxMembers,
		int currentMembers,
		String status,
		UserSummaryResponse createdBy,
		boolean joinable,
		String joinRestriction,
		Instant createdAt
	) {
		private static RoomSummaryResponse from(RoomListItem item) {
			String restriction = item.currentMembers() >= item.maxMembers() ? "FULL" : null;
			return new RoomSummaryResponse(
				item.id(),
				item.name(),
				item.description(),
				new RoomCategoryResponse(
					item.categoryId(),
					item.categoryName(),
					item.categoryDescription(),
					item.categorySortOrder()
				),
				item.workStyle(),
				item.maxMembers(),
				item.currentMembers(),
				item.status(),
				new UserSummaryResponse(item.creatorId(), item.creatorName(), item.creatorIconUrl()),
				restriction == null,
				restriction,
				item.createdAt()
			);
		}
	}

	public record RoomDetailResponse(
		long id,
		String name,
		String description,
		RoomCategoryResponse category,
		String workStyle,
		int maxMembers,
		int currentMembers,
		String status,
		UserSummaryResponse createdBy,
		boolean joinable,
		String joinRestriction,
		boolean member,
		Instant createdAt,
		Instant updatedAt
	) {
		static RoomDetailResponse from(RoomView room) {
			String restriction = "OPEN".equals(room.status())
				? (room.currentMembers() >= room.maxMembers() ? "FULL" : null)
				: "CLOSED";
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
				room.currentMembers(),
				room.status(),
				new UserSummaryResponse(room.creatorId(), room.creatorName(), room.creatorIconUrl()),
				restriction == null,
				restriction,
				room.isMember(),
				room.createdAt(),
				room.updatedAt()
			);
		}
	}

	public record RoomCategoryResponse(long id, String name, String description, int sortOrder) {
	}

	public record UserSummaryResponse(long id, String name, String iconUrl) {
	}

	public record RoomCreatedEvent(String type, RoomDetailResponse payload) {
	}
}
