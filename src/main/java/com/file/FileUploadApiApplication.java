package com.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileUploadApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileUploadApiApplication.class, args);
	}

}
