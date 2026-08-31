package com.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.file.config.FiloraFSProperties;

@SpringBootTest(properties = "spring.config.import=")
class FileUploadApiApplicationTests {

	@TempDir
	static Path storageRoot;

	@DynamicPropertySource
	static void configureStorage(DynamicPropertyRegistry registry) {
		registry.add("filorafs.storage-path", () -> storageRoot.resolve("uploads").toString());
		registry.add("filorafs.api-key", () -> "context-test-key");
	}

	@Autowired
	private FiloraFSProperties properties;

	@Test
	void contextLoadsWithBoundConfigurationAndCreatesStorage() {
		assertEquals(storageRoot.resolve("uploads").toString(), properties.storagePath());
		assertEquals("context-test-key", properties.apiKey());
		assertTrue(Files.isDirectory(storageRoot.resolve("uploads")));
	}
}
