package com.file.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.file.config.FiloraFSProperties;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "filorafs.api-key=test-api-key",
    "filorafs.storage-path=./target/test-uploads"
})
class FileControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FiloraFSProperties properties;

    @TempDir
    Path tempStorageRoot;

    private Path originalStorage;

    @BeforeEach
    void setup() throws Exception {
        // Change storage path for tests safely (or let it use defaults but tests isolate API logic)
        // Spring properties are read-only after start, but for controller testing we mostly test API-KEY and routing.
        // For file endpoints, we use the autoconfigured ones which might write to default ./uploads.
        // Since we didn't mock service, we test the actual application context.
    }

    @Test
    void testMissingApiKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/file/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testInvalidApiKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/file/list").header("X-API-KEY", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testApiTestAccessibleWithoutKey() throws Exception {
        // Since /api-test/index.html is a static resource, the ApiKeyFilter should ignore it.
        mockMvc.perform(get("/api-test/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void testValidApiKeyAllowsAccess() throws Exception {
        mockMvc.perform(get("/file/list").header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk());
    }
    
    @Test
    void testUploadFileValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.png", "image/png", "image bytes".getBytes());

        mockMvc.perform(multipart("/file").file(file).header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk());
    }

    @Test
    void testUploadFileUnsupportedType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.sh", "application/x-sh", "echo 'hello'".getBytes());

        mockMvc.perform(multipart("/file").file(file).header("X-API-KEY", "test-api-key"))
                .andExpect(status().isBadRequest());
    }
}
