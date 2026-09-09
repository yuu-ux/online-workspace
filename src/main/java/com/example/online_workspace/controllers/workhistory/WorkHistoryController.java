package com.example.online_workspace.controllers.workhistory;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.services.WorkHistoryService;
import com.example.online_workspace.services.WorkHistoryService.Result;
import com.example.online_workspace.services.WorkHistoryService.WorkHistory;

@RestController
@RequestMapping("/api/v1/workhistories")
public class WorkHistoryController {

	private final WorkHistoryService service;

	public WorkHistoryController(WorkHistoryService service) {
		this.service = service;
	}

	@GetMapping
	public WorkHistoryPageResponse list(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		Authentication authentication
	) {
		Result result = service.list(authentication.getName(), page, size);
		long totalPages = result.totalElements() / size
			+ (result.totalElements() % size == 0 ? 0 : 1);
		return new WorkHistoryPageResponse(
			result.items().stream().map(WorkHistoryResponse::from).toList(),
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

	public record WorkHistoryPageResponse(List<WorkHistoryResponse> items, PageMetaResponse page) {
	}

	public record WorkHistoryResponse(
		long roomId,
		String roomName,
		Instant joinedAt,
		Instant leftAt,
		long durationMinutes
	) {
		private static WorkHistoryResponse from(WorkHistory item) {
			return new WorkHistoryResponse(
				item.roomId(),
				item.roomName(),
				item.joinedAt(),
				item.leftAt(),
				item.durationMinutes()
			);
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
