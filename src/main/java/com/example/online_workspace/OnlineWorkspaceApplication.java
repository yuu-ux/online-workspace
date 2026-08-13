package com.example.online_workspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OnlineWorkspaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineWorkspaceApplication.class, args);
	}

}
