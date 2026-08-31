package com.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "filorafs")
public record FiloraFSProperties(
		String storagePath,
		String apiKey
) {

	public FiloraFSProperties {
		if (storagePath == null || storagePath.isBlank()) {
			throw new IllegalArgumentException("filorafs.storage-path must not be blank");
		}
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalArgumentException("filorafs.api-key must not be blank");
		}
	}
}
