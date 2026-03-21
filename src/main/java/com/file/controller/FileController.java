package com.file.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.file.dtos.FileStream;
import com.file.services.FileService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/file")
public class FileController {

	private FileService fileService;

	public FileController(FileService fileService) {
		super();
		this.fileService = fileService;
	}

	@PostMapping
	public String saveFile(@RequestPart MultipartFile file) throws IOException {
		return this.fileService.saveFile(file);
	}

	@DeleteMapping("/{filename}")
	public boolean deleteFile(@PathVariable String filename) {
		return this.fileService.deleteFile(filename);
	}

	@GetMapping("/{filename}")
	public void getFile(@PathVariable String filename, HttpServletResponse response)
			throws FileNotFoundException, IOException {
		try {
			FileStream stream = this.fileService.getFile(filename);

			response.setContentType(stream.getMimeType());

			StreamUtils.copy(stream.getIs(), response.getOutputStream());
			stream.getIs().close();
			response.getOutputStream().flush();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@GetMapping("/list")
	public List<String> listFiles() {
		return fileService.listFiles();
	}

	@GetMapping("/info/{filename}")
	public Map<String, Object> getFileInfo(@PathVariable String filename) throws IOException {
		return fileService.getFileMetadata(filename);
	}

}
