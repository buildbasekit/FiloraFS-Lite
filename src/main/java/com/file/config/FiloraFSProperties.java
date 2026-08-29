package com.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "filorafs")
public record FiloraFSProperties(
    String storagePath,
    String apiKey
) {}
