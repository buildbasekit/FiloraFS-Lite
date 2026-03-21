package com.file.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.file.dtos.FileStream;

@Service
public class FileService {
	@Value("${upload.path}")
	private String path;

	private List<String> allowedFileType = List.of("image/png", "image/jpeg", "application/pdf");

	public String saveFile(MultipartFile file) throws IOException {
		String mimeType = file.getContentType();
		if (!this.allowedFileType.contains(mimeType)) {
			throw new RuntimeException("File type not allowed: " + mimeType);
		}
		File directory = new File(path.trim());
		if (!directory.exists()) {
			directory.mkdir();
		}
		String filename = UUID.randomUUID().toString()
				.concat(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));
		String filepath = this.getFullPath(filename);
		File dest = new File(filepath);
		file.transferTo(dest);
		return filename;
	}

	public FileStream getFile(String fileName) throws FileNotFoundException {
		try {
			String filepath = this.getFullPath(fileName);
			File file = new File(filepath);
			if (!file.exists()) {
				String defaultFilePath = this.getFullPath("default.png");
				file = new File(defaultFilePath);
			}
			String mimeType = Files.probeContentType(file.toPath());
			if (mimeType == null) {
				mimeType = "application/octet-stream";
			}

			return new FileStream(new FileInputStream(file), mimeType);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public boolean deleteFile(String fileName) {
		String filepath = path.trim() + File.separator + fileName;
		File file = new File(filepath);
		if (file.exists()) {
			return file.delete();
		} else {
			return false;
		}
	}

	public List<String> listFiles() {
		File folder = new File(path.trim());
		File[] files = folder.listFiles();
		if (files == null)
			return Collections.emptyList();
		return Arrays.stream(files).filter(File::isFile).map(File::getName).collect(Collectors.toList());
	}

	public Map<String, Object> getFileMetadata(String fileName) throws IOException {
		File file = new File(getFullPath(fileName));
		Map<String, Object> info = new HashMap<>();
		info.put("name", file.getName());
		info.put("sizeKB", file.length() / 1024);
		info.put("mimeType", Files.probeContentType(file.toPath()));
		info.put("lastModified", Files.getLastModifiedTime(file.toPath()).toString());
		return info;
	}

	// private functions

	private String getFullPath(String fileName) {
		return path.trim() + File.separator + fileName;
	}

	// scheduled tasks
	@Scheduled(cron = "0 0 1 * * ?") // every day at 1 AM
	public void cleanOldFiles() {
		File folder = new File(path.trim());
		long cutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // 7 days
		File[] oldFiles = folder.listFiles(file -> file.isFile() && file.lastModified() < cutoff);
		for (File f : oldFiles)
			f.delete();
	}

}
