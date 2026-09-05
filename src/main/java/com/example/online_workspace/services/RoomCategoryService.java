package com.example.online_workspace.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.models.RoomCategory;
import com.example.online_workspace.repositories.RoomCategoryRepository;

@Service
public class RoomCategoryService {

	private final RoomCategoryRepository repository;

	public RoomCategoryService(RoomCategoryRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<RoomCategory> list() {
		return repository.findActive();
	}
}
