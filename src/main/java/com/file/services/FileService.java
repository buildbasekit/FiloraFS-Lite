package com.file.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import com.file.config.FiloraFSProperties;
import com.file.dtos.FileMetadata;

import jakarta.annotation.PostConstruct;

@Service
public class FileService {

	private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
			".png", "image/png",
			".jpg", "image/jpeg",
			".jpeg", "image/jpeg",
			".pdf", "application/pdf",
			".webp", "image/webp");

	private final Path storageRoot;

	public FileService(FiloraFSProperties properties) {
		this.storageRoot = Path.of(properties.storagePath()).toAbsolutePath().normalize();
	}

	@PostConstruct
	public void init() {
		try {
			Files.createDirectories(storageRoot);
			if (!Files.isDirectory(storageRoot)) {
				throw new IllegalStateException("Configured storage location is not a directory");
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not initialize storage location", e);
		}
	}

	public String saveFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
		}

		String extension = getAllowedExtension(file.getOriginalFilename());
		String expectedMimeType = ALLOWED_EXTENSIONS.get(extension);
		String declaredMimeType = file.getContentType();
		if (declaredMimeType == null || !expectedMimeType.equals(declaredMimeType.toLowerCase(Locale.ROOT))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type does not match its extension");
		}

		String filename = UUID.randomUUID().toString() + extension;
		Path destination = resolvePathSafely(filename);

		try {
			if (!expectedMimeType.equals(detectMimeType(file))) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content does not match its type");
			}
			// Reserve the generated name atomically: transferTo alone may overwrite an existing file.
			Files.createFile(destination);
			try {
				file.transferTo(destination);
			} catch (IOException | IllegalStateException e) {
				try {
					Files.deleteIfExists(destination);
				} catch (IOException cleanupFailure) {
					e.addSuppressed(cleanupFailure);
				}
				throw e;
			}
			return filename;
		} catch (IOException | IllegalStateException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file", e);
		}
	}

	public Resource getFileAsResource(String fileName) {
		Path filePath = resolvePathSafely(fileName);
		if (!Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}
		return new FileSystemResource(filePath);
	}

	public boolean deleteFile(String fileName) {
		Path filePath = resolvePathSafely(fileName);
		if (!Files.exists(filePath, LinkOption.NOFOLLOW_LINKS)) {
			return false;
		}
		if (!Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stored item is not a regular file");
		}
		try {
			return Files.deleteIfExists(filePath);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete file", e);
		}
	}

	public List<String> listFiles() {
		try (Stream<Path> stream = Files.list(storageRoot)) {
			return stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
					.map(path -> path.getFileName().toString())
					.sorted()
					.toList();
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not list files");
		}
	}

	public FileMetadata getFileMetadata(String fileName) {
		Path filePath = resolvePathSafely(fileName);
		if (!Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}

		try {
			String mimeType = MediaTypeFactory.getMediaType(fileName)
					.orElse(MediaType.APPLICATION_OCTET_STREAM).toString();

			String lastModified = DateTimeFormatter.ISO_INSTANT.format(
					Files.getLastModifiedTime(filePath, LinkOption.NOFOLLOW_LINKS).toInstant());

			return new FileMetadata(
					filePath.getFileName().toString(),
					Files.size(filePath) / 1024,
					mimeType,
					lastModified
			);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file metadata");
		}
	}

	private Path resolvePathSafely(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
		}

		try {
			Path suppliedPath = Path.of(fileName);
			if (suppliedPath.isAbsolute() || suppliedPath.getNameCount() != 1
					|| fileName.contains("/") || fileName.contains("\\")) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Path traversal is not allowed");
			}

			Path requestedPath = storageRoot.resolve(suppliedPath).normalize();
			if (!requestedPath.startsWith(storageRoot) || requestedPath.equals(storageRoot)
					|| !storageRoot.equals(requestedPath.getParent())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Path traversal is not allowed");
			}
			return requestedPath;
		} catch (InvalidPathException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name", e);
		}
	}

	private String getAllowedExtension(String originalFilename) {
		if (originalFilename == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name must include an allowed extension");
		}
		int extensionStart = originalFilename.lastIndexOf('.');
		if (extensionStart < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name must include an allowed extension");
		}
		String extension = originalFilename.substring(extensionStart).toLowerCase(Locale.ROOT);
		if (!ALLOWED_EXTENSIONS.containsKey(extension)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File extension is not allowed");
		}
		return extension;
	}

	private String detectMimeType(MultipartFile file) throws IOException {
		byte[] header;
		try (InputStream inputStream = file.getInputStream()) {
			header = inputStream.readNBytes(12);
		}

		if (startsWith(header, new byte[] { (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a })) {
			return "image/png";
		}
		if (startsWith(header, new byte[] { (byte) 0xff, (byte) 0xd8, (byte) 0xff })) {
			return "image/jpeg";
		}
		if (startsWith(header, new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2d })) {
			return "application/pdf";
		}
		if (header.length >= 12 && startsWith(header, new byte[] { 0x52, 0x49, 0x46, 0x46 })
				&& header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
			return "image/webp";
		}
		return null;
	}

	private boolean startsWith(byte[] value, byte[] prefix) {
		if (value.length < prefix.length) {
			return false;
		}
		for (int index = 0; index < prefix.length; index++) {
			if (value[index] != prefix[index]) {
				return false;
			}
		}
		return true;
	}
}
