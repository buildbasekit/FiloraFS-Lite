package com.file.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.file.dtos.FileMetadata;
import com.file.services.FileService;

@RestController
@RequestMapping("/file")
public class FileController {

	private final FileService fileService;

	public FileController(FileService fileService) {
		this.fileService = fileService;
	}

	@PostMapping
	public String saveFile(@RequestPart("file") MultipartFile file) {
		return this.fileService.saveFile(file);
	}

	@DeleteMapping("/{filename}")
	public boolean deleteFile(@PathVariable String filename) {
		return this.fileService.deleteFile(filename);
	}

	@GetMapping("/{filename}")
	public ResponseEntity<Resource> getFile(@PathVariable String filename) {
		Resource resource = this.fileService.getFileAsResource(filename);

		return ResponseEntity.ok()
				.contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(resource.getFilename(), StandardCharsets.UTF_8)
						.build()
						.toString())
				.body(resource);
	}

	@GetMapping("/list")
	public List<String> listFiles() {
		return fileService.listFiles();
	}

	@GetMapping("/info/{filename}")
	public FileMetadata getFileInfo(@PathVariable String filename) {
		return fileService.getFileMetadata(filename);
	}

}
