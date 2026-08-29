package com.example.online_workspace.services;

import java.time.Instant;
import java.util.List;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.exceptions.FieldErrorResponse;
import com.example.online_workspace.forms.ProfileUpdateRequest;
import com.example.online_workspace.repositories.ProfileRepository;
import com.example.online_workspace.repositories.ProfileRepository.ProfileRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

	private final ProfileRepository repository;

	public ProfileService(ProfileRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public ProfileResponse updateMine(String email, ProfileUpdateRequest request) {
		Long userId = repository.lockActiveUserIdByEmail(email);
		if (userId == null) {
			throw unauthorized();
		}
		if (request.workCategoryId() != null
			&& !repository.activeCategoryExists(request.workCategoryId())) {
			throw new ApiException(
				HttpStatus.UNPROCESSABLE_CONTENT,
				"VALIDATION_FAILED",
				"入力内容を確認してください。",
				List.of(new FieldErrorResponse(
					"workCategoryId",
					"INVALID",
					"作業カテゴリが存在しません。"
				))
			);
		}

		repository.updateName(userId, request.name().trim());
		repository.upsertProfile(
			userId,
			request.iconUrl(),
			request.bio(),
			request.workCategoryId(),
			request.isPublic()
		);

		return repository.findByEmail(email)
			.map(this::toResponse)
			.orElseThrow(this::unauthorized);
	}

	private ProfileResponse toResponse(ProfileRow row) {
		RoomCategory category = row.workCategoryId() == null
			? null
			: new RoomCategory(
				row.workCategoryId(),
				row.workCategoryName(),
				row.workCategoryDescription(),
				row.workCategorySortOrder()
			);
		return new ProfileResponse(
			row.userId(),
			row.name(),
			row.iconUrl(),
			row.isPublic(),
			row.bio(),
			category,
			"NONE",
			false,
			row.email(),
			row.role(),
			row.accountStatus(),
			row.createdAt()
		);
	}

	private ResponseStatusException unauthorized() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です。");
	}

	public record ProfileResponse(
		long id,
		String name,
		String iconUrl,
		boolean isPublic,
		String bio,
		RoomCategory workCategory,
		String friendship,
		boolean blocked,
		String email,
		String role,
		String accountStatus,
		Instant createdAt
	) {
	}

	public record RoomCategory(long id, String name, String description, int sortOrder) {
	}
}
