package com.file.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.file.config.FiloraFSProperties;
import com.file.dtos.FileMetadata;

class FileServiceTests {

	private FileService fileService;

	@TempDir
	Path tempStorageRoot;

	@BeforeEach
	void setUp() {
		FiloraFSProperties properties = new FiloraFSProperties(tempStorageRoot.toString(), "test-key");
		fileService = new FileService(properties);
		fileService.init();
	}

	@Test
	void testInitCreatesDirectory() {
		assertTrue(Files.exists(tempStorageRoot));
	}

	@Test
	void testSaveFileSuccess() throws IOException {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.png", "image/png", "dummy-content".getBytes());
		
		String savedName = fileService.saveFile(file);
		assertNotNull(savedName);
		assertTrue(savedName.endsWith(".png"));
		
		Path savedPath = tempStorageRoot.resolve(savedName);
		assertTrue(Files.exists(savedPath));
	}

	@Test
	void testSaveFileInvalidType() {
		MockMultipartFile file = new MockMultipartFile(
				"file", "test.txt", "text/plain", "dummy-content".getBytes());
		
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileService.saveFile(file));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testPathTraversalProtection() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileService.getFileAsResource("../secret.txt"));
		assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        
        ResponseStatusException ex3 = assertThrows(ResponseStatusException.class, () -> fileService.getFileAsResource("nested/../folder/../../secret.txt"));
		assertEquals(HttpStatus.FORBIDDEN, ex3.getStatusCode());
	}

	@Test
	void testAbsolutePathRejected() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileService.getFileAsResource("/etc/passwd"));
		assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
	}

	@Test
	void testEmptyFilenameRejected() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileService.getFileAsResource(""));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testNullFilenameRejected() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileService.getFileAsResource(null));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testDeletePathTraversalBlocked() {
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileService.deleteFile("../secret.txt"));
		assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
	}

	@Test
	void testEmptyFileRejected() {
		MockMultipartFile file = new MockMultipartFile(
				"file", "empty.png", "image/png", new byte[0]);
		
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> fileService.saveFile(file));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testDeleteFile() throws IOException {
		Path testFile = tempStorageRoot.resolve("delete-me.png");
		Files.writeString(testFile, "data");
		
		assertTrue(fileService.deleteFile("delete-me.png"));
		assertFalse(Files.exists(testFile));
	}
	
	@Test
	void testDeleteMissingFileReturnsFalse() {
		assertFalse(fileService.deleteFile("missing.png"));
	}

	@Test
	void testListFiles() throws IOException {
		Files.writeString(tempStorageRoot.resolve("file1.png"), "data");
		Files.writeString(tempStorageRoot.resolve("file2.jpg"), "data");
		
		List<String> files = fileService.listFiles();
		assertEquals(2, files.size());
		assertTrue(files.contains("file1.png"));
		assertTrue(files.contains("file2.jpg"));
	}

	@Test
	void testGetFileMetadata() throws IOException {
		Path testFile = tempStorageRoot.resolve("meta.png");
		Files.writeString(testFile, "12345");
		
		FileMetadata metadata = fileService.getFileMetadata("meta.png");
		assertEquals("meta.png", metadata.name());
		assertNotNull(metadata.lastModified());
	}
}
