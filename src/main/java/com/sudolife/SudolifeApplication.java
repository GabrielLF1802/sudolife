package com.sudolife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SudolifeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SudolifeApplication.class, args);
	}

}
