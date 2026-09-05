package com.example.online_workspace.repositories.users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import com.example.online_workspace.repositories.users.UserRepository.MyProfileRow;

@MybatisTest(properties =
	"spring.datasource.url=jdbc:h2:mem:my-profile-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Sql("/my-profile-repository-test.sql")
class UserRepositoryTests {

	@Autowired
	private UserRepository repository;

	@Test
	void returnsDefaultsWithoutAProfileRow() {
		MyProfileRow profile = repository.findMyProfileByEmail("me@example.com");

		assertThat(profile).isNotNull();
		assertThat(profile.iconUrl()).isNull();
		assertThat(profile.bio()).isEmpty();
		assertThat(profile.isPublic()).isTrue();
		assertThat(profile.categoryId()).isNull();
	}
}
