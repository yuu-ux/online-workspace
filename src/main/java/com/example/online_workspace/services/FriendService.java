package com.example.online_workspace.services;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.repositories.FriendRepository;
import com.example.online_workspace.repositories.FriendRepository.FriendRow;
import com.example.online_workspace.repositories.users.UserRepository;

@Service
public class FriendService {

	private final FriendRepository repository;
	private final UserRepository userRepository;

	public FriendService(FriendRepository repository, UserRepository userRepository) {
		this.repository = repository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public Result list(String email, int page, int size) {
		Long userId = userRepository.findActiveUserIdByEmail(email);
		if (userId == null) {
			throw unauthorized();
		}
		long totalElements = repository.countActiveFriends(userId);
		long offset = (long) page * size;
		List<Friend> items = offset >= totalElements
			? List.of()
			: repository.findActiveFriends(userId, size, offset).stream().map(this::toFriend).toList();
		return new Result(items, totalElements);
	}

	@Transactional
	public Friend create(String email, long friendUserId) {
		Long userId = userRepository.findActiveUserIdByEmail(email);
		if (userId == null) {
			throw unauthorized();
		}
		if (userId == friendUserId) {
			throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "自分自身はフレンド追加できません。");
		}
		if (!repository.activeUserExists(friendUserId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "ユーザーが見つかりません。");
		}
		if (repository.existsActive(userId, friendUserId)) {
			throw new ApiException(HttpStatus.CONFLICT, "FRIEND_ALREADY_EXISTS", "既にフレンドです。");
		}

		upsertActivePair(userId, friendUserId);
		FriendRow created = repository.findActiveFriend(userId, friendUserId);
		if (created == null) {
			throw new IllegalStateException("Friend row was not created");
		}
		return toFriend(created);
	}

	@Transactional
	public void delete(String email, long friendUserId) {
		Long userId = userRepository.findActiveUserIdByEmail(email);
		if (userId == null) {
			throw unauthorized();
		}
		if (!repository.existsActive(userId, friendUserId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "FRIEND_NOT_FOUND", "フレンドが見つかりません。");
		}
		repository.markRemoved(userId, friendUserId);
		repository.markRemoved(friendUserId, userId);
	}

	private void upsertActivePair(long userId, long friendUserId) {
		int updatedForward = repository.reactivate(userId, friendUserId);
		if (updatedForward == 0) {
			repository.insertActive(userId, friendUserId);
		}
		int updatedReverse = repository.reactivate(friendUserId, userId);
		if (updatedReverse == 0) {
			repository.insertActive(friendUserId, userId);
		}
	}

	private Friend toFriend(FriendRow row) {
		return new Friend(
			new UserSummary(row.friendUserId(), row.friendName(), row.friendIconUrl()),
			false,
			row.createdAt()
		);
	}

	private ApiException unauthorized() {
		return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
	}

	public record Result(List<Friend> items, long totalElements) {
	}

	public record Friend(UserSummary user, boolean online, Instant createdAt) {
	}

	public record UserSummary(long id, String name, String iconUrl) {
	}
}
