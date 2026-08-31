package com.file.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
	private Path storageRoot;

	@TempDir
	Path temporaryDirectory;

	@BeforeEach
	void setUp() {
		storageRoot = temporaryDirectory.resolve("uploads");
		fileService = new FileService(new FiloraFSProperties(storageRoot.toString(), "test-key"));
		fileService.init();
	}

	@Test
	void initCreatesConfiguredDirectory() {
		assertTrue(Files.isDirectory(storageRoot));
	}

	@Test
	void saveFileUsesGeneratedNameAndPreservesBytes() throws IOException {
		byte[] content = pngBytes();
		MockMultipartFile file = new MockMultipartFile("file", "example.PNG", "image/png", content);

		String savedName = fileService.saveFile(file);

		assertTrue(savedName.matches("[0-9a-f-]{36}\\.png"));
		assertArrayEquals(content, Files.readAllBytes(storageRoot.resolve(savedName)));
	}

	@Test
	void repeatedOriginalNamesDoNotOverwriteStoredFiles() throws IOException {
		MockMultipartFile file = new MockMultipartFile("file", "../../example.png", "image/png", pngBytes());
		String first = fileService.saveFile(file);
		String second = fileService.saveFile(file);

		assertNotEquals(first, second);
		assertEquals(2, fileService.listFiles().size());
		assertArrayEquals(pngBytes(), Files.readAllBytes(storageRoot.resolve(first)));
		assertArrayEquals(pngBytes(), Files.readAllBytes(storageRoot.resolve(second)));
	}

	@Test
	void failedTransferRemovesPartialFile() {
		MockMultipartFile file = new MockMultipartFile("file", "example.png", "image/png", pngBytes()) {
			@Override
			public void transferTo(Path destination) throws IOException {
				Files.write(destination, new byte[] { 1 });
				throw new IOException("Simulated storage failure");
			}
		};

		assertStatus(HttpStatus.INTERNAL_SERVER_ERROR, () -> fileService.saveFile(file));
		assertTrue(fileService.listFiles().isEmpty());
	}

	@Test
	void symbolicLinksCannotReadDeleteOrExposeOutsideFiles() throws IOException {
		Path outsideFile = temporaryDirectory.resolve("outside.png");
		Files.write(outsideFile, pngBytes());
		try {
			Files.createSymbolicLink(storageRoot.resolve("linked.png"), outsideFile);
		} catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
			assumeTrue(false, "Symbolic links unavailable: " + e.getClass().getSimpleName());
		}

		assertStatus(HttpStatus.NOT_FOUND, () -> fileService.getFileAsResource("linked.png"));
		assertStatus(HttpStatus.NOT_FOUND, () -> fileService.getFileMetadata("linked.png"));
		assertStatus(HttpStatus.BAD_REQUEST, () -> fileService.deleteFile("linked.png"));
		assertTrue(fileService.listFiles().isEmpty());
		assertArrayEquals(pngBytes(), Files.readAllBytes(outsideFile));
	}

	@Test
	void saveFileRejectsEmptyUpload() {
		MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

		assertStatus(HttpStatus.BAD_REQUEST, () -> fileService.saveFile(file));
	}

	@Test
	void saveFileRejectsUnsupportedExtension() {
		MockMultipartFile file = new MockMultipartFile("file", "script.html", "image/png", pngBytes());

		assertStatus(HttpStatus.BAD_REQUEST, () -> fileService.saveFile(file));
	}

	@Test
	void saveFileRejectsDeclaredTypeThatDoesNotMatchExtension() {
		MockMultipartFile file = new MockMultipartFile("file", "document.pdf", "image/png", pngBytes());

		assertStatus(HttpStatus.BAD_REQUEST, () -> fileService.saveFile(file));
	}

	@Test
	void saveFileRejectsSpoofedContent() {
		MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "not an image".getBytes());

		assertStatus(HttpStatus.BAD_REQUEST, () -> fileService.saveFile(file));
	}

	@Test
	void getFileReturnsExistingResourceBytes() throws IOException {
		Path storedFile = storageRoot.resolve("stored.png");
		Files.write(storedFile, pngBytes());

		Resource resource = fileService.getFileAsResource("stored.png");

		assertTrue(resource.exists());
		assertArrayEquals(pngBytes(), resource.getContentAsByteArray());
	}

	@Test
	void getFileReturnsNotFoundForMissingFile() {
		assertStatus(HttpStatus.NOT_FOUND, () -> fileService.getFileAsResource("missing.png"));
	}

	@Test
	void pathTraversalAndAbsolutePathsAreRejected() {
		List<String> unsafeNames = List.of(
				"../secret.txt",
				"..\\secret.txt",
				"nested/../secret.txt",
				"nested\\..\\secret.txt",
				"/etc/passwd",
				"C:\\Windows\\win.ini");

		for (String unsafeName : unsafeNames) {
			assertStatus(HttpStatus.FORBIDDEN, () -> fileService.getFileAsResource(unsafeName));
			assertStatus(HttpStatus.FORBIDDEN, () -> fileService.deleteFile(unsafeName));
			assertStatus(HttpStatus.FORBIDDEN, () -> fileService.getFileMetadata(unsafeName));
		}
	}

	@Test
	void blankAndInvalidNamesAreRejected() {
		assertStatus(HttpStatus.BAD_REQUEST, () -> fileService.getFileAsResource(""));
		assertStatus(HttpStatus.BAD_REQUEST, () -> fileService.getFileAsResource(null));
	}

	@Test
	void deleteExistingAndMissingFilesHasStableBooleanContract() throws IOException {
		Path storedFile = storageRoot.resolve("delete-me.png");
		Files.write(storedFile, pngBytes());

		assertTrue(fileService.deleteFile("delete-me.png"));
		assertFalse(Files.exists(storedFile));
		assertFalse(fileService.deleteFile("missing.png"));
	}

	@Test
	void deleteRejectsDirectories() throws IOException {
		Files.createDirectory(storageRoot.resolve("directory.png"));

		assertStatus(HttpStatus.BAD_REQUEST, () -> fileService.deleteFile("directory.png"));
	}

	@Test
	void listReturnsOnlyRegularFilesInSortedOrder() throws IOException {
		Files.write(storageRoot.resolve("b.jpg"), jpegBytes());
		Files.write(storageRoot.resolve("a.png"), pngBytes());
		Files.createDirectory(storageRoot.resolve("directory"));

		assertEquals(List.of("a.png", "b.jpg"), fileService.listFiles());
	}

	@Test
	void listReturnsEmptyListForEmptyStorage() {
		assertTrue(fileService.listFiles().isEmpty());
	}

	@Test
	void metadataReturnsSafeFields() throws IOException {
		Path storedFile = storageRoot.resolve("meta.png");
		Files.write(storedFile, pngBytes());

		FileMetadata metadata = fileService.getFileMetadata("meta.png");

		assertEquals("meta.png", metadata.name());
		assertTrue(metadata.sizeKB() >= 0);
		assertNotNull(metadata.mimeType());
		assertNotNull(metadata.lastModified());
	}

	@Test
	void metadataReturnsNotFoundForMissingFile() {
		assertStatus(HttpStatus.NOT_FOUND, () -> fileService.getFileMetadata("missing.png"));
	}

	private void assertStatus(HttpStatus expected, ThrowingOperation operation) {
		ResponseStatusException exception = assertThrows(ResponseStatusException.class, operation::run);
		assertEquals(expected, exception.getStatusCode());
	}

	private static byte[] pngBytes() {
		return new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01 };
	}

	private static byte[] jpegBytes() {
		return new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01 };
	}

	@FunctionalInterface
	private interface ThrowingOperation {
		void run() throws Exception;
	}
}
