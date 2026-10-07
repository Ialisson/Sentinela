package com.sentinela;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRabbit
@EnableScheduling
public class SentinelaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentinelaApplication.class, args);
	}
}
