package com.example.online_workspace.controllers.users;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties =
	"spring.datasource.url=jdbc:h2:mem:profile-update;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@Sql(scripts = "/profile-update-test.sql")
class ProfileControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String email;

	@BeforeEach
	void setUp() {
		email = "profile-" + UUID.randomUUID() + "@example.com";
		jdbcTemplate.update(
			"INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
			"更新前",
			email,
			"password-hash"
		);
	}

	@Test
	void updateAndClearProfile() throws Exception {
		jdbcTemplate.update(
			"INSERT INTO room_categories (name, description, sort_order) VALUES (?, ?, ?)",
			"開発",
			"ソフトウェア開発",
			10
		);
		Long categoryId = jdbcTemplate.queryForObject(
			"SELECT id FROM room_categories WHERE name = '開発'",
			Long.class
		);

		mockMvc.perform(put("/api/v1/users/me/profile")
				.with(user(email))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
					{"name":" 更新後 ","iconUrl":"https://example.com/icon.png","bio":"自己紹介","workCategoryId":%d,"isPublic":false}
					""".formatted(categoryId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("更新後"))
			.andExpect(jsonPath("$.iconUrl").value("https://example.com/icon.png"))
			.andExpect(jsonPath("$.bio").value("自己紹介"))
			.andExpect(jsonPath("$.workCategory.id").value(categoryId))
			.andExpect(jsonPath("$.workCategory.name").value("開発"))
			.andExpect(jsonPath("$.isPublic").value(false));

		mockMvc.perform(put("/api/v1/users/me/profile")
				.with(user(email))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
					{"name":"更新後","iconUrl":null,"bio":"","workCategoryId":null,"isPublic":true}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.iconUrl").doesNotExist())
			.andExpect(jsonPath("$.workCategory").doesNotExist())
			.andExpect(jsonPath("$.isPublic").value(true));
	}

	@Test
	void rejectInvalidProfile() throws Exception {
		mockMvc.perform(put("/api/v1/users/me/profile")
				.with(user(email))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
					{"name":" ","iconUrl":"ftp://example.com/icon.png","bio":"","workCategoryId":null,"isPublic":true}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors.length()").value(2));
	}

	@Test
	void rejectUnknownCategory() throws Exception {
		mockMvc.perform(put("/api/v1/users/me/profile")
				.with(user(email))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
					{"name":"更新後","iconUrl":null,"bio":"","workCategoryId":999999,"isPublic":true}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("workCategoryId"))
			.andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("作業カテゴリが存在しません。"));
	}

	@Test
	void requireAuthenticationAndCsrf() throws Exception {
		mockMvc.perform(put("/api/v1/users/me/profile")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
					{"name":"更新後","iconUrl":null,"bio":"","workCategoryId":null,"isPublic":true}
					"""))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(put("/api/v1/users/me/profile")
				.with(user(email))
				.contentType(APPLICATION_JSON)
				.content("""
					{"name":"更新後","iconUrl":null,"bio":"","workCategoryId":null,"isPublic":true}
					"""))
			.andExpect(status().isForbidden());
	}
}
