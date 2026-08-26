package com.example.online_workspace.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.models.RoomDraft;
import com.example.online_workspace.repositories.RoomRepository;
import com.example.online_workspace.repositories.RoomRepository.RoomView;

@Service
public class RoomService {

	private final RoomRepository repository;

	public RoomService(RoomRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public RoomView create(String creatorEmail, CreateRoomCommand command) {
		Long creatorId = repository.findActiveUserIdByEmail(creatorEmail);
		if (creatorId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		if (!repository.isActiveCategory(command.categoryId())) {
			throw new ApiException(
				HttpStatus.BAD_REQUEST,
				"INVALID_CATEGORY",
				"利用可能なカテゴリを指定してください。"
			);
		}

		RoomDraft room = new RoomDraft(
			command.name(),
			command.description(),
			creatorId,
			command.categoryId(),
			command.workStyle(),
			command.maxMembers()
		);
		if (repository.insert(room) != 1 || room.getId() == null) {
			throw new IllegalStateException("Room was not created");
		}
		if (repository.insertCreatorMembership(room.getId(), creatorId) != 1) {
			throw new IllegalStateException("Creator membership was not created");
		}

		RoomView created = repository.findById(room.getId());
		if (created == null) {
			throw new IllegalStateException("Created room was not found");
		}
		return created;
	}

	public record CreateRoomCommand(
		String name,
		String description,
		long categoryId,
		String workStyle,
		int maxMembers
	) {
	}
}
