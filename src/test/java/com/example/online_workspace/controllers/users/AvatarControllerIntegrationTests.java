package com.example.online_workspace.controllers.users;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:avatar-upload;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"app.avatar.storage-dir=build/test-avatars"
})
@AutoConfigureMockMvc
@Sql(scripts = "/profile-update-test.sql")
class AvatarControllerIntegrationTests {

	private static final byte[] PNG = Base64.getDecoder().decode(
		"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
	);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String email;

	@BeforeEach
	void setUp() {
		email = "avatar-" + UUID.randomUUID() + "@example.com";
		jdbcTemplate.update(
			"INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
			"アイコン利用者",
			email,
			"password-hash"
		);
	}

	@Test
	void uploadsAndServesAnAvatar() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file", "avatar.png", MediaType.IMAGE_PNG_VALUE, PNG
		);

		String iconUrl = mockMvc.perform(multipart("/api/v1/users/me/avatar")
				.file(file)
				.with(user(email))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.iconUrl").value(matchesPattern("/api/v1/users/[0-9]+/avatar")))
			.andReturn()
			.getResponse()
			.getContentAsString()
			.replaceAll(".*\\\"iconUrl\\\":\\\"([^\\\"]+)\\\".*", "$1");

		Long userId = jdbcTemplate.queryForObject(
			"SELECT id FROM users WHERE email = ?", Long.class, email
		);
		org.junit.jupiter.api.Assertions.assertEquals(
			iconUrl,
			jdbcTemplate.queryForObject("SELECT icon_url FROM profiles WHERE user_id = ?", String.class, userId)
		);

		mockMvc.perform(get(iconUrl).with(user(email)))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
			.andExpect(content().bytes(PNG));
	}

	@Test
	void rejectsNonImageFiles() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file", "avatar.txt", MediaType.TEXT_PLAIN_VALUE, "not an image".getBytes()
		);

		mockMvc.perform(multipart("/api/v1/users/me/avatar")
				.file(file)
				.with(user(email))
				.with(csrf()))
			.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void deletesAnAvatar() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file", "avatar.png", MediaType.IMAGE_PNG_VALUE, PNG
		);

		String iconUrl = mockMvc.perform(multipart("/api/v1/users/me/avatar")
				.file(file)
				.with(user(email))
				.with(csrf()))
			.andReturn()
			.getResponse()
			.getContentAsString()
			.replaceAll(".*\\\"iconUrl\\\":\\\"([^\\\"]+)\\\".*", "$1");

		mockMvc.perform(delete("/api/v1/users/me/avatar")
				.with(user(email))
				.with(csrf()))
			.andExpect(status().isNoContent());

		Long userId = jdbcTemplate.queryForObject(
			"SELECT id FROM users WHERE email = ?", Long.class, email
		);
		org.junit.jupiter.api.Assertions.assertNull(
			jdbcTemplate.queryForObject("SELECT icon_url FROM profiles WHERE user_id = ?", String.class, userId)
		);
		mockMvc.perform(get(iconUrl).with(user(email)))
			.andExpect(status().isNotFound());
	}
}
