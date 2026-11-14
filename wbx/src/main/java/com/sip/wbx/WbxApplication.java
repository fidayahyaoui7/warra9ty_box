package com.sip.wbx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WbxApplication {

	public static void main(String[] args) {
		SpringApplication.run(WbxApplication.class, args);
	}

}
