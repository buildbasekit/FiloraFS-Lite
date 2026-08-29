package com.file.dtos;

public record FileMetadata(
    String name,
    long sizeKB,
    String mimeType,
    String lastModified
) {}
