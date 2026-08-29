package com.example.online_workspace.controllers.users;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.services.MyProfileService;
import com.example.online_workspace.services.MyProfileService.MyProfile;

@RestController
@RequestMapping("/api/v1/users/me/profile")
public class MyProfileController {

	private final MyProfileService service;

	public MyProfileController(MyProfileService service) {
		this.service = service;
	}

	@GetMapping
	public MyProfile get(Authentication authentication) {
		return service.get(authentication.getName());
	}
}
