package com.example.online_workspace.controllers.users;

import com.example.online_workspace.forms.ProfileUpdateRequest;
import com.example.online_workspace.services.MyProfileService;
import com.example.online_workspace.services.MyProfileService.MyProfile;
import com.example.online_workspace.services.ProfileService;
import com.example.online_workspace.services.ProfileService.ProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/profile")
public class ProfileController {

	private final ProfileService service;
	private final MyProfileService myProfileService;

	public ProfileController(ProfileService service, MyProfileService myProfileService) {
		this.service = service;
		this.myProfileService = myProfileService;
	}

	@GetMapping
	public MyProfile get(Authentication authentication) {
		return myProfileService.get(authentication.getName());
	}

	@PutMapping
	public ProfileResponse update(
		@Valid @RequestBody ProfileUpdateRequest request,
		Authentication authentication
	) {
		return service.updateMine(authentication.getName(), request);
	}
}
