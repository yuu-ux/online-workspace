package com.example.online_workspace.configs.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("ログイン画面は未認証でも表示できる")
	@Test
	void loginPageIsAvailable() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk());
	}

	@DisplayName("未認証のWebアクセスはログイン画面へリダイレクトする")
	@Test
	void unauthenticatedWebRequestRedirectsToLoginPage() throws Exception {
		mockMvc.perform(get("/protected-page"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
	}
}
