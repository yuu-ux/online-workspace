package com.example.online_workspace.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.models.RoomDraft;
import com.example.online_workspace.repositories.RoomRepository;
import com.example.online_workspace.repositories.RoomRepository.RoomState;
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

		RoomView created = repository.findById(room.getId(), creatorId);
		if (created == null) {
			throw new IllegalStateException("Created room was not found");
		}
		return created;
	}

	@Transactional(readOnly = true)
	public RoomView get(String email, long roomId) {
		Long viewerId = repository.findActiveUserIdByEmail(email);
		if (viewerId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		RoomView room = repository.findById(roomId, viewerId);
		if (room == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "ルームが見つかりません。");
		}
		return room;
	}

	@Transactional
	public RoomView update(String email, long roomId, UpdateRoomCommand command) {
		Long editorId = repository.findActiveUserIdByEmail(email);
		if (editorId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		RoomState state = repository.findRoomState(roomId);
		if (state == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "ルームが見つかりません。");
		}
		if (state.creatorId() != editorId) {
			throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "この操作を行う権限がありません。");
		}
		if (!"OPEN".equals(state.status())) {
			throw new ApiException(HttpStatus.CONFLICT, "ROOM_NOT_OPEN", "終了済みのルームは更新できません。");
		}
		if (!repository.isActiveCategory(command.categoryId())) {
			throw new ApiException(
				HttpStatus.BAD_REQUEST,
				"INVALID_CATEGORY",
				"利用可能なカテゴリを指定してください。"
			);
		}
		if (state.currentMembers() > command.maxMembers()) {
			throw new ApiException(
				HttpStatus.CONFLICT,
				"MAX_MEMBERS_BELOW_CURRENT",
				"現在の参加人数より少ない定員には変更できません。"
			);
		}

		int updated = repository.updateRoom(
			roomId,
			command.name(),
			command.description(),
			command.categoryId(),
			command.workStyle(),
			command.maxMembers()
		);
		if (updated != 1) {
			throw new IllegalStateException("Room was not updated");
		}
		RoomView room = repository.findById(roomId, editorId);
		if (room == null) {
			throw new IllegalStateException("Updated room was not found");
		}
		return room;
	}

	@Transactional
	public void close(String email, long roomId) {
		Long operatorId = repository.findActiveUserIdByEmail(email);
		if (operatorId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		RoomState state = repository.findRoomState(roomId);
		if (state == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "ルームが見つかりません。");
		}
		if (state.creatorId() != operatorId) {
			throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "この操作を行う権限がありません。");
		}
		if (!"OPEN".equals(state.status())) {
			throw new ApiException(HttpStatus.CONFLICT, "ROOM_NOT_OPEN", "終了済みのルームは終了できません。");
		}
		if (repository.closeRoom(roomId) != 1) {
			throw new IllegalStateException("Room was not closed");
		}
	}

	public record CreateRoomCommand(
		String name,
		String description,
		long categoryId,
		String workStyle,
		int maxMembers
	) {
	}

	public record UpdateRoomCommand(
		String name,
		String description,
		long categoryId,
		String workStyle,
		int maxMembers
	) {
	}
}
