package com.example.online_workspace.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.models.RoomListItem;
import com.example.online_workspace.repositories.RoomListRepository;

@Service
public class RoomListService {

	private final RoomListRepository repository;

	public RoomListService(RoomListRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public Result list(String email, Long categoryId, String workStyle, int page, int size) {
		if (email == null || repository.findActiveUserIdByEmail(email) == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}

		long totalElements = repository.countOpenRooms(categoryId, workStyle);
		long offset = (long) page * size;
		boolean requestedPageIsOutOfRange = offset >= totalElements;
		List<RoomListItem> items = requestedPageIsOutOfRange
			? List.of()
			: repository.findOpenRooms(categoryId, workStyle, size, offset);
		return new Result(items, totalElements);
	}

	public record Result(List<RoomListItem> items, long totalElements) {
	}
}
