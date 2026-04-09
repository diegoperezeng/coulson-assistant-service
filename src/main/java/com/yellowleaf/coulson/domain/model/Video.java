package com.yellowleaf.coulson.domain.model;

import java.nio.file.Path;
import java.util.UUID;

public class Video {

    private final String id;
    private final String originalName;
    private final Path filePath;
    private final long sizeInBytes;
    private final String contentType;
    private double durationSeconds;

    public Video(String originalName, Path filePath, long sizeInBytes, String contentType) {
        this.id = UUID.randomUUID().toString();
        this.originalName = originalName;
        this.filePath = filePath;
        this.sizeInBytes = sizeInBytes;
        this.contentType = contentType;
    }

    public String getId() {
        return id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getFileExtension() {
        String name = originalName;
        int dotIndex = name.lastIndexOf('.');
        return dotIndex >= 0 ? name.substring(dotIndex) : ".mp4";
    }
}
