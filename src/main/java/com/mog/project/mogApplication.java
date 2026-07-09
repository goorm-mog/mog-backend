package com.mog.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class mogApplication {

	public static void main(String[] args) {
		SpringApplication.run(mogApplication.class, args);
	}

}
