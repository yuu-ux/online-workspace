package com.example.online_workspace.controllers.rooms;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.models.RoomCategory;
import com.example.online_workspace.services.RoomCategoryService;

@RestController
@RequestMapping("/api/v1/room-categories")
public class RoomCategoryController {

	private final RoomCategoryService service;

	public RoomCategoryController(RoomCategoryService service) {
		this.service = service;
	}

	@GetMapping
	public List<RoomCategory> list() {
		return service.list();
	}
}
