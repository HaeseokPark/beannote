package com.beanote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BeanoteApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeanoteApplication.class, args);
	}

}
