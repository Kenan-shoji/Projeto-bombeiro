package com.example.bombeiros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BombeirosApplication {

	public static void main(String[] args) {
		SpringApplication.run(BombeirosApplication.class, args);
	}

}
