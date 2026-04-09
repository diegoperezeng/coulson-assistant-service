package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.Video;
import com.yellowleaf.coulson.domain.port.VideoStoragePort;
import com.yellowleaf.coulson.infrastructure.config.VideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class LocalVideoStorageAdapter implements VideoStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalVideoStorageAdapter.class);

    private final VideoProperties videoProperties;

    public LocalVideoStorageAdapter(VideoProperties videoProperties) {
        this.videoProperties = videoProperties;
    }

    @Override
    public Video store(MultipartFile file) {
        try {
            Path tempDir = Paths.get(videoProperties.getTempDir());
            Files.createDirectories(tempDir);

            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "video.mp4";

            String extension = extractExtension(originalName);
            String storedName = UUID.randomUUID() + "-original" + extension;
            Path destination = tempDir.resolve(storedName);

            file.transferTo(destination);

            log.info("Video stored at: {}", destination);
            return new Video(originalName, destination, file.getSize(), file.getContentType());

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store video file", e);
        }
    }

    @Override
    public void delete(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filePath);
        }
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : ".mp4";
    }
}
