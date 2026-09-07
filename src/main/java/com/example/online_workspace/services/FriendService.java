package com.example.online_workspace.services;

import java.time.Instant;
import java.util.List;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.models.users.AuthenticatedUser;
import com.example.online_workspace.repositories.FriendRepository;
import com.example.online_workspace.repositories.FriendRepository.FriendRow;
import com.example.online_workspace.repositories.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

	private final FriendRepository friendRepository;
	private final UserRepository userRepository;
	private final OnlinePresenceService onlinePresenceService;

	public FriendService(
		FriendRepository friendRepository,
		UserRepository userRepository,
		OnlinePresenceService onlinePresenceService
	) {
		this.friendRepository = friendRepository;
		this.userRepository = userRepository;
		this.onlinePresenceService = onlinePresenceService;
	}

	@Transactional(readOnly = true)
	public Result list(String email, int page, int size) {
		long userId = activeUserId(email);
		long totalElements = friendRepository.countActiveFriends(userId);
		long offset = (long) page * size;
		List<FriendItem> items = offset >= totalElements
			? List.of()
			: friendRepository.findActiveFriends(userId, size, offset).stream()
				.map(this::toItem)
				.toList();
		return new Result(items, totalElements);
	}

	@Transactional
	public FriendItem add(String email, long friendUserId) {
		long userId = activeUserId(email);
		if (userId == friendUserId) {
			throw new ApiException(HttpStatus.CONFLICT, "SELF_FRIEND", "自分自身はフレンドに追加できません。");
		}
		if (friendRepository.findActiveTargetById(friendUserId) == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "FRIEND_USER_NOT_FOUND", "フレンド対象のユーザーが見つかりません。");
		}
		if (friendRepository.existsActiveFriend(userId, friendUserId)) {
			throw new ApiException(HttpStatus.CONFLICT, "ALREADY_FRIEND", "既にフレンドです。");
		}

		Long existingFriendId = friendRepository.findFriendId(userId, friendUserId);
		if (existingFriendId == null) {
			friendRepository.insertFriend(userId, friendUserId);
		} else {
			friendRepository.reactivateFriend(existingFriendId);
		}
		return toItem(friendRepository.findActiveFriend(userId, friendUserId));
	}

	@Transactional
	public void delete(String email, long friendUserId) {
		long userId = activeUserId(email);
		if (friendRepository.removeFriend(userId, friendUserId) == 0) {
			throw new ApiException(HttpStatus.NOT_FOUND, "FRIEND_NOT_FOUND", "フレンドが見つかりません。");
		}
	}

	private long activeUserId(String email) {
		return userRepository.findActiveAuthenticatedByEmail(email)
			.map(AuthenticatedUser::id)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。"));
	}

	private FriendItem toItem(FriendRow row) {
		return new FriendItem(
			row.friendId(),
			row.friendName(),
			row.friendEmail(),
			row.friendIconUrl(),
			onlinePresenceService.isOnline(row.friendEmail()),
			row.createdAt()
		);
	}

	public record Result(List<FriendItem> items, long totalElements) {
	}

	public record FriendItem(
		long id,
		String name,
		String email,
		String iconUrl,
		boolean online,
		Instant createdAt
	) {
	}
}
