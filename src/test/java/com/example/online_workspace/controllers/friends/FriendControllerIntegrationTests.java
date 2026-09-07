package com.example.online_workspace.controllers.friends;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:friend-controller-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@Sql(scripts = "/friend-controller-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FriendControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@DisplayName("フレンドを追加して一覧で確認できる")
	@Test
	void createsAndListsFriend() throws Exception {
		String ownerEmail = uniqueEmail();
		String friendEmail = uniqueEmail();
		insertUser(ownerEmail, "フレンド所有者");
		long friendId = insertUser(friendEmail, "追加対象");
		MockHttpSession session = login(ownerEmail);
		Cookie csrf = csrfCookie(session);

		mockMvc.perform(post("/api/v1/friends")
				.session(session)
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue())
				.contentType(APPLICATION_JSON)
				.content("{\"userId\":%d}".formatted(friendId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.user.id").value(friendId))
			.andExpect(jsonPath("$.user.name").value("追加対象"))
			.andExpect(jsonPath("$.online").value(false));

		mockMvc.perform(get("/api/v1/friends").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].user.id").value(friendId))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@DisplayName("フレンドを解除すると一覧から消える")
	@Test
	void deletesFriend() throws Exception {
		String ownerEmail = uniqueEmail();
		String friendEmail = uniqueEmail();
		insertUser(ownerEmail, "フレンド所有者");
		long friendId = insertUser(friendEmail, "解除対象");
		MockHttpSession session = login(ownerEmail);
		Cookie csrf = csrfCookie(session);
		addFriend(session, csrf, friendId);

		mockMvc.perform(delete("/api/v1/friends/{friendUserId}", friendId)
				.session(session)
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue()))
			.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/friends").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isEmpty())
			.andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@DisplayName("同じフレンドを重複追加できない")
	@Test
	void rejectsDuplicateFriend() throws Exception {
		String ownerEmail = uniqueEmail();
		String friendEmail = uniqueEmail();
		insertUser(ownerEmail, "フレンド所有者");
		long friendId = insertUser(friendEmail, "追加対象");
		MockHttpSession session = login(ownerEmail);
		Cookie csrf = csrfCookie(session);
		addFriend(session, csrf, friendId);

		mockMvc.perform(post("/api/v1/friends")
				.session(session)
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue())
				.contentType(APPLICATION_JSON)
				.content("{\"userId\":%d}".formatted(friendId)))
			.andExpect(status().isConflict());
	}

	@DisplayName("解除済みのユーザーを再びフレンドに追加できる")
	@Test
	void reactivatesRemovedFriend() throws Exception {
		String ownerEmail = uniqueEmail();
		String friendEmail = uniqueEmail();
		insertUser(ownerEmail, "フレンド所有者");
		long friendId = insertUser(friendEmail, "再追加対象");
		MockHttpSession session = login(ownerEmail);
		Cookie csrf = csrfCookie(session);
		addFriend(session, csrf, friendId);

		mockMvc.perform(delete("/api/v1/friends/{friendUserId}", friendId)
				.session(session)
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue()))
			.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/friends")
				.session(session)
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue())
				.contentType(APPLICATION_JSON)
				.content("{\"userId\":%d}".formatted(friendId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.user.id").value(friendId));
	}

	@DisplayName("存在しないユーザーはフレンドに追加できない")
	@Test
	void rejectsUnknownFriend() throws Exception {
		String ownerEmail = uniqueEmail();
		insertUser(ownerEmail, "フレンド所有者");
		MockHttpSession session = login(ownerEmail);
		Cookie csrf = csrfCookie(session);

		mockMvc.perform(post("/api/v1/friends")
				.session(session)
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue())
				.contentType(APPLICATION_JSON)
				.content("{\"userId\":999999999}"))
			.andExpect(status().isNotFound());
	}

	private void addFriend(MockHttpSession session, Cookie csrf, long friendId) throws Exception {
		mockMvc.perform(post("/api/v1/friends")
				.session(session)
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue())
				.contentType(APPLICATION_JSON)
				.content("{\"userId\":%d}".formatted(friendId)))
			.andExpect(status().isCreated());
	}

	private MockHttpSession login(String email) throws Exception {
		Cookie csrf = csrfCookie(null);
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue())
				.contentType(APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"password-123\"}".formatted(email)))
			.andExpect(status().isOk())
			.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertNotNull(session);
		return session;
	}

	private Cookie csrfCookie(MockHttpSession session) throws Exception {
		MvcResult result = session == null
			? mockMvc.perform(get("/api/v1/auth/csrf")).andReturn()
			: mockMvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn();
		Cookie csrf = result.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(csrf);
		return csrf;
	}

	private long insertUser(String email, String name) {
		jdbcTemplate.update(
			"INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
			name,
			email,
			passwordEncoder.encode("password-123")
		);
		return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
	}

	private String uniqueEmail() {
		return "friend-" + UUID.randomUUID() + "@example.com";
	}
}
