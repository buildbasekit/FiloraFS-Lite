package com.file.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
class FileControllerTests {

	private static final String API_KEY = "test-api-key";

	@TempDir
	static Path storageRoot;

	@DynamicPropertySource
	static void configureApplication(DynamicPropertyRegistry registry) {
		registry.add("filorafs.api-key", () -> API_KEY);
		registry.add("filorafs.storage-path", storageRoot::toString);
	}

	@Autowired
	private MockMvc mockMvc;

	@AfterEach
	void clearStorage() throws IOException {
		try (var files = Files.list(storageRoot)) {
			for (Path path : files.toList()) {
				if (Files.isRegularFile(path)) {
					Files.delete(path);
				}
			}
		}
	}

	@Test
	void missingAndInvalidApiKeysReturnUnauthorized() throws Exception {
		mockMvc.perform(get("/file/list"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string("Unauthorized"));

		mockMvc.perform(get("/file/list").header("X-API-KEY", "wrong-key"))
				.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@ValueSource(strings = { "/file;param=value/list", "/%66ile/list", "/file%2Flist" })
	void encodedAndMatrixPathsCannotBypassApiKey(String path) throws Exception {
		mockMvc.perform(get(URI.create(path))).andExpect(status().isUnauthorized());
	}

	@Test
	void apiKeyProtectionWorksWithContextPath() throws Exception {
		mockMvc.perform(get("/app/file/list").contextPath("/app"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/app/file/list").contextPath("/app").header("X-API-KEY", API_KEY))
				.andExpect(status().isOk());
	}

	@Test
	void healthIsPublicWithoutDetailsAndOtherActuatorEndpointsAreNotExposed() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"UP\"}"))
				.andExpect(jsonPath("$.components").doesNotExist())
				.andExpect(jsonPath("$.details").doesNotExist());
		for (String path : new String[] { "/actuator", "/actuator/env", "/actuator/configprops",
				"/actuator/heapdump", "/actuator/info", "/actuator/shutdown" }) {
			mockMvc.perform(get(path)).andExpect(status().isNotFound());
		}
	}

	@Test
	void unknownStoredExtensionUsesOctetStreamForDownloadAndMetadata() throws Exception {
		byte[] bytes = new byte[] { 1, 2, 3 };
		Files.write(storageRoot.resolve("legacy.unknown-type"), bytes);
		mockMvc.perform(get("/file/legacy.unknown-type").header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/octet-stream"))
				.andExpect(content().bytes(bytes));
		mockMvc.perform(get("/file/info/legacy.unknown-type").header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mimeType").value("application/octet-stream"));
	}

	@Test
	void validApiKeyAllowsFileApi() throws Exception {
		mockMvc.perform(get("/file/list").header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void apiTestRoutesAndResourcesArePublic() throws Exception {
		mockMvc.perform(get("/api-test"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/api-test/index.html"));
		mockMvc.perform(get("/api-test/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("FiloraFS-Lite API Console")));
		mockMvc.perform(get("/api-test/styles.css")).andExpect(status().isOk());
		mockMvc.perform(get("/api-test/app.js")).andExpect(status().isOk());
	}

	@Test
	void uploadListMetadataDownloadAndDeleteWorkflow() throws Exception {
		byte[] fileBytes = pngBytes();
		MockMultipartFile file = new MockMultipartFile("file", "hello.png", "image/png", fileBytes);

		MvcResult upload = mockMvc.perform(multipart("/file").file(file).header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andReturn();
		String filename = upload.getResponse().getContentAsString();

		mockMvc.perform(get("/file/list").header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasItem(filename)));
		mockMvc.perform(get("/file/info/{filename}", filename).header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(filename))
				.andExpect(jsonPath("$.mimeType").value("image/png"))
				.andExpect(jsonPath("$.lastModified").isNotEmpty());
		mockMvc.perform(get("/file/{filename}", filename).header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andExpect(content().bytes(fileBytes))
				.andExpect(content().contentType("image/png"))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
		mockMvc.perform(delete("/file/{filename}", filename).header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andExpect(content().string("true"));
		mockMvc.perform(get("/file/{filename}", filename).header("X-API-KEY", API_KEY))
				.andExpect(status().isNotFound());
	}

	@Test
	void uploadRejectsEmptyUnsupportedAndSpoofedFiles() throws Exception {
		MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
		MockMultipartFile unsupported = new MockMultipartFile("file", "script.sh", "application/x-sh", "echo".getBytes());
		MockMultipartFile spoofed = new MockMultipartFile("file", "fake.png", "image/png", "plain text".getBytes());

		mockMvc.perform(multipart("/file").file(empty).header("X-API-KEY", API_KEY))
				.andExpect(status().isBadRequest());
		mockMvc.perform(multipart("/file").file(unsupported).header("X-API-KEY", API_KEY))
				.andExpect(status().isBadRequest());
		mockMvc.perform(multipart("/file").file(spoofed).header("X-API-KEY", API_KEY))
				.andExpect(status().isBadRequest());
	}

	@Test
	void missingFilesHaveDocumentedBehavior() throws Exception {
		mockMvc.perform(get("/file/missing.png").header("X-API-KEY", API_KEY))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/file/info/missing.png").header("X-API-KEY", API_KEY))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/file/missing.png").header("X-API-KEY", API_KEY))
				.andExpect(status().isOk())
				.andExpect(content().string("false"));
	}

	private static byte[] pngBytes() {
		return new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01 };
	}
}
