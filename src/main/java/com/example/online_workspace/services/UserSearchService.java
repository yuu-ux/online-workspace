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
public class UserSearchService {

	private final UserRepository repository;

	public UserSearchService(UserRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public Result search(String email, String query, int page, int size) {
		Long viewerId = repository.findActiveUserIdByEmail(email);
		if (email == null || viewerId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}

		long totalElements = repository.countByNameLike(viewerId, query);
		long offset = (long) page * size;
		List<UserSummaryRow> items = offset >= totalElements
			? List.of()
			: repository.findByNameLike(viewerId, query, size, offset);
		return new Result(items, totalElements);
	}

	@Transactional(readOnly = true)
	public UserProfile getProfile(String email, long targetUserId) {
		Long viewerId = repository.findActiveUserIdByEmail(email);
		if (email == null || viewerId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		if (viewerId == targetUserId) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "自身のプロフィールはこのAPIでは取得できません。");
		}

		UserProfileRow row = repository.findUserProfileById(viewerId, targetUserId);
		if (row == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "ユーザーが見つかりません。");
		}

		RoomCategory category = row.workCategoryId() == null
			? null
			: new RoomCategory(
				row.workCategoryId(),
				row.workCategoryName(),
				row.workCategoryDescription(),
				row.workCategorySortOrder()
			);
		return new UserProfile(
			row.id(),
			row.name(),
			row.iconUrl(),
			row.isPublic(),
			row.bio(),
			category,
			row.friendship()
		);
	}

	public record Result(List<UserSummaryRow> items, long totalElements) {
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
