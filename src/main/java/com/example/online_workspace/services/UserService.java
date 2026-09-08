package com.example.online_workspace.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.repositories.users.UserRepository;
import com.example.online_workspace.repositories.users.UserRepository.UserProfileRow;
import com.example.online_workspace.repositories.users.UserRepository.UserSummaryRow;

@Service
public class UserService {

	private final UserRepository repository;

	public UserService(UserRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public Result search(String email, String query, int page, int size) {
		if (repository.findActiveUserIdByEmail(email) == null) {
			throw unauthorized();
		}
		long totalElements = repository.countByNameLike(query);
		long offset = (long) page * size;
		List<UserSummary> items = offset >= totalElements
			? List.of()
			: repository.findByNameLike(query, size, offset).stream().map(this::toSummary).toList();
		return new Result(items, totalElements);
	}

	@Transactional(readOnly = true)
	public UserProfile getUser(String email, long userId) {
		Long requesterId = repository.findActiveUserIdByEmail(email);
		if (requesterId == null) {
			throw unauthorized();
		}
		UserProfileRow row = repository.findUserProfileById(requesterId, userId);
		if (row == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "ユーザーが見つかりません。");
		}
		RoomCategory category = row.categoryId() == null ? null : new RoomCategory(
			row.categoryId(),
			row.categoryName(),
			row.categoryDescription(),
			row.categorySortOrder()
		);
		return new UserProfile(
			row.id(),
			row.name(),
			row.iconUrl(),
			row.isPublic(),
			row.bio(),
			category,
			row.friend() ? "FRIEND" : "NONE"
		);
	}

	private UserSummary toSummary(UserSummaryRow row) {
		return new UserSummary(row.id(), row.name(), row.iconUrl());
	}

	private ApiException unauthorized() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
	}

	public record Result(List<UserSummary> items, long totalElements) {
	}

	public record UserSummary(long id, String name, String iconUrl) {
	}

	public record UserProfile(
		long id,
		String name,
		String iconUrl,
		boolean isPublic,
		String bio,
		RoomCategory workCategory,
		String friendship
	) {
	}

	public record RoomCategory(long id, String name, String description, int sortOrder) {
	}
}
