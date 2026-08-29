package com.example.online_workspace.services;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.repositories.users.UserRepository;
import com.example.online_workspace.repositories.users.UserRepository.MyProfileRow;

@Service
public class MyProfileService {

	private final UserRepository repository;

	public MyProfileService(UserRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public MyProfile get(String email) {
		MyProfile row = toProfile(repository.findMyProfileByEmail(email));
		if (row == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		return row;
	}

	private MyProfile toProfile(MyProfileRow row) {
		if (row == null) {
			return null;
		}
		RoomCategory category = row.categoryId() == null ? null : new RoomCategory(
			row.categoryId(),
			row.categoryName(),
			row.categoryDescription(),
			row.categorySortOrder()
		);
		return new MyProfile(
			row.id(), row.name(), row.iconUrl(), row.isPublic(), row.bio(), category,
			"NONE", false, row.email(), row.role(), row.accountStatus(), row.createdAt()
		);
	}

	public record MyProfile(
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
