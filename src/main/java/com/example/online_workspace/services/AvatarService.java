package com.example.online_workspace.services;

import com.example.online_workspace.repositories.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AvatarService {

	private final ProfileRepository repository;
	private final AvatarStorageService storage;

	public AvatarService(ProfileRepository repository, AvatarStorageService storage) {
		this.repository = repository;
		this.storage = storage;
	}

	@Transactional
	public AvatarResponse upload(String email, MultipartFile file) {
		long userId = activeUserId(email);
		storage.save(userId, file);
		String iconUrl = avatarUrl(userId);
		if (repository.updateIconUrl(userId, iconUrl) == 0) {
			repository.insertIconUrl(userId, iconUrl);
		}
		return new AvatarResponse(iconUrl);
	}

	private long activeUserId(String email) {
		Long userId = repository.lockActiveUserIdByEmail(email);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です。");
		}
		return userId;
	}

	public String avatarUrl(long userId) {
		return "/api/v1/users/" + userId + "/avatar";
	}

	public record AvatarResponse(String iconUrl) {
	}
}
