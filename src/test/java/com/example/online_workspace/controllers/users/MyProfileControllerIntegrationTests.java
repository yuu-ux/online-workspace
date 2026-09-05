package com.example.online_workspace.controllers.users;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.online_workspace.repositories.users.UserRepository;
import com.example.online_workspace.repositories.users.UserRepository.MyProfileRow;

@SpringBootTest
@AutoConfigureMockMvc
class MyProfileControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository repository;

	@Test
	void returnsTheAuthenticatedUsersProfile() throws Exception {
		when(repository.findMyProfileByEmail("me@example.com")).thenReturn(new MyProfileRow(
			10L, "自分", "https://example.com/me.png", true, "朝に集中して作業します",
			20L, "開発", "プログラミング", 1, "me@example.com", "USER", "ACTIVE",
			Instant.parse("2026-08-01T00:00:00Z")
		));

		mockMvc.perform(get("/api/v1/users/me/profile").with(user("me@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.name").value("自分"))
			.andExpect(jsonPath("$.iconUrl").value("https://example.com/me.png"))
			.andExpect(jsonPath("$.bio").value("朝に集中して作業します"))
			.andExpect(jsonPath("$.workCategory.name").value("開発"))
			.andExpect(jsonPath("$.friendship").value("NONE"))
			.andExpect(jsonPath("$.blocked").value(false))
			.andExpect(jsonPath("$.email").value("me@example.com"))
			.andExpect(jsonPath("$.role").value("USER"))
			.andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
	}

	@Test
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/profile"))
			.andExpect(status().isUnauthorized());
	}
}
