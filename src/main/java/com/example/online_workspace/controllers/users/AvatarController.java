package com.example.online_workspace.controllers.users;

import java.io.IOException;

import com.example.online_workspace.services.AvatarService;
import com.example.online_workspace.services.AvatarStorageService;
import com.example.online_workspace.services.AvatarStorageService.StoredAvatar;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class AvatarController {

	private final AvatarService service;
	private final AvatarStorageService storage;

	public AvatarController(AvatarService service, AvatarStorageService storage) {
		this.service = service;
		this.storage = storage;
	}

	@PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AvatarService.AvatarResponse upload(
		@RequestPart("file") MultipartFile file,
		Authentication authentication
	) {
		return service.upload(authentication.getName(), file);
	}

	@DeleteMapping("/me/avatar")
	public ResponseEntity<Void> delete(Authentication authentication) {
		service.delete(authentication.getName());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{userId}/avatar")
	public ResponseEntity<Resource> get(@PathVariable long userId) {
		return storage.find(userId)
			.map(this::response)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private ResponseEntity<Resource> response(StoredAvatar avatar) {
		try {
			return ResponseEntity.ok()
				.contentType(avatar.mediaType())
				.contentLength(java.nio.file.Files.size(avatar.path()))
				.body(new FileSystemResource(avatar.path()));
		} catch (IOException exception) {
			return ResponseEntity.internalServerError().build();
		}
	}
}
