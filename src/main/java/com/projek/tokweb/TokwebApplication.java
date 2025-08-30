package com.projek.tokweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// @EnableConfigurationProperties(CheckoutProperties.class)
@EnableScheduling
public class TokwebApplication {

	public static void main(String[] args) {
		SpringApplication.run(TokwebApplication.class, args);
	}
}
