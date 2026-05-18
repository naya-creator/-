package com.EcommerceApp.H2NS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
@SpringBootApplication
@EnableAsync
public class H2NsApplication {

	public static void main(String[] args) {
		SpringApplication.run(H2NsApplication.class, args);
	}

}
