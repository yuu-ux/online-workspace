package com.example.online_workspace.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.repositories.users.UserRepository;
import com.example.online_workspace.repositories.users.UserRepository.UserSummaryRow;

@Service
public class UserSearchService {

	private final UserRepository repository;

	public UserSearchService(UserRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public Result search(String email, String query, int page, int size) {
		if (email == null || repository.findActiveUserIdByEmail(email) == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}

		long totalElements = repository.countByNameLike(query);
		long offset = (long) page * size;
		List<UserSummaryRow> items = offset >= totalElements
			? List.of()
			: repository.findByNameLike(query, size, offset);
		return new Result(items, totalElements);
	}

	public record Result(List<UserSummaryRow> items, long totalElements) {
	}
}
