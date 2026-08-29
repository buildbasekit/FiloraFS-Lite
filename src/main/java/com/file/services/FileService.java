package com.file.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.file.config.FiloraFSProperties;
import com.file.dtos.FileMetadata;

import jakarta.annotation.PostConstruct;

@Service
public class FileService {

	private final FiloraFSProperties properties;
	private final Path storageRoot;
	private final List<String> allowedFileType = List.of("image/png", "image/jpeg", "application/pdf", "image/webp");

	public FileService(FiloraFSProperties properties) {
		this.properties = properties;
		this.storageRoot = Paths.get(properties.storagePath()).toAbsolutePath().normalize();
	}

	@PostConstruct
	public void init() {
		try {
			if (!Files.exists(storageRoot)) {
				Files.createDirectories(storageRoot);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize storage location", e);
		}
	}

	public String saveFile(MultipartFile file) throws IOException {
		String mimeType = file.getContentType();
		if (mimeType == null || !this.allowedFileType.contains(mimeType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type not allowed: " + mimeType);
		}
		if (file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
		}

		String originalFilename = file.getOriginalFilename();
		String extension = "";
		if (originalFilename != null && originalFilename.contains(".")) {
			extension = originalFilename.substring(originalFilename.lastIndexOf("."));
		}

		String filename = UUID.randomUUID().toString() + extension;
		Path dest = resolvePathSafely(filename);

		file.transferTo(dest);
		return filename;
	}

	public Resource getFileAsResource(String fileName) {
		Path filePath = resolvePathSafely(fileName);
		if (!Files.exists(filePath)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}
		return new FileSystemResource(filePath);
	}

	public boolean deleteFile(String fileName) {
		Path filePath = resolvePathSafely(fileName);
		try {
			return Files.deleteIfExists(filePath);
		} catch (IOException e) {
			return false;
		}
	}

	public List<String> listFiles() {
		try (Stream<Path> stream = Files.list(storageRoot)) {
			return stream.filter(Files::isRegularFile)
					.map(path -> path.getFileName().toString())
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not list files");
		}
	}

	public FileMetadata getFileMetadata(String fileName) {
		Path filePath = resolvePathSafely(fileName);
		if (!Files.exists(filePath)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}

		try {
			String mimeType = Files.probeContentType(filePath);
			if (mimeType == null) mimeType = "application/octet-stream";

			String lastModified = DateTimeFormatter.ISO_INSTANT
					.format(Files.getLastModifiedTime(filePath).toInstant());

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
		Path requestedPath = storageRoot.resolve(fileName).normalize();
		if (!requestedPath.startsWith(storageRoot)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Path traversal is not allowed");
		}
		return requestedPath;
	}
}
