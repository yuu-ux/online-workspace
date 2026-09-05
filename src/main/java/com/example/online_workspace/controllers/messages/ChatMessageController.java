package com.example.online_workspace.controllers.messages;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.models.ChatMessage;
import com.example.online_workspace.services.ChatMessageService;
import com.example.online_workspace.services.ChatMessageService.Result;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/messages")
public class ChatMessageController {

	private final ChatMessageService service;

	public ChatMessageController(ChatMessageService service) {
		this.service = service;
	}

	@GetMapping
	public MessagePageResponse list(
		@PathVariable long roomId,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
		Authentication authentication
	) {
		Result result = service.list(roomId, authentication.getName(), page, size);
		long totalPages = result.totalElements() / size
			+ (result.totalElements() % size == 0 ? 0 : 1);
		return new MessagePageResponse(
			result.items(),
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
	public ChatMessage create(
		@PathVariable long roomId,
		@Valid @RequestBody CreateMessageRequest request,
		Authentication authentication
	) {
		ChatMessage message = service.create(roomId, authentication.getName(), request.content());
		service.publish(message);
		return message;
	}

	public record CreateMessageRequest(
		@NotBlank(message = "メッセージを入力してください。")
		@Size(max = 500, message = "メッセージは500文字以内で入力してください。")
		String content
	) {
	}

	public record MessagePageResponse(List<ChatMessage> items, PageMetaResponse page) {
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
