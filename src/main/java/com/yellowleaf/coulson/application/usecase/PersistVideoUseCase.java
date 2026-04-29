package com.yellowleaf.coulson.application.usecase;

import com.yellowleaf.coulson.domain.model.VideoAsset;
import com.yellowleaf.coulson.domain.model.VideoAssetStatus;
import com.yellowleaf.coulson.domain.port.VideoAssetRepositoryPort;
import com.yellowleaf.coulson.domain.port.VideoAssetStoragePort;
import com.yellowleaf.coulson.domain.port.VideoTranscodePort;
import com.yellowleaf.coulson.domain.port.VideoTranscodePort.TranscodeResult;
import com.yellowleaf.coulson.infrastructure.config.VideoPersistProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PersistVideoUseCase {

    private static final Logger log = LoggerFactory.getLogger(PersistVideoUseCase.class);

    private final VideoPersistProperties properties;
    private final VideoAssetRepositoryPort videoAssetRepository;
    private final VideoTranscodePort transcodePort;
    private final VideoAssetStoragePort videoStoragePort;
    private final Executor persistExecutor;
    private final String tempDir;

    public PersistVideoUseCase(VideoPersistProperties properties,
                               VideoAssetRepositoryPort videoAssetRepository,
                               VideoTranscodePort transcodePort,
                               VideoAssetStoragePort videoStoragePort,
                               Executor persistExecutor,
                               String tempDir) {
        this.properties = properties;
        this.videoAssetRepository = videoAssetRepository;
        this.transcodePort = transcodePort;
        this.videoStoragePort = videoStoragePort;
        this.persistExecutor = persistExecutor;
        this.tempDir = tempDir;
    }

    public void persistAsync(String refId, UUID teamId, Path inputPath) {
        if (!properties.isEnabled()) {
            deleteQuietly(inputPath);
            return;
        }

        Optional<VideoAsset> existing = videoAssetRepository.findByRefId(refId);

        if (existing.isPresent()) {
            VideoAsset asset = existing.get();

            if (asset.getStatus() == VideoAssetStatus.READY) {
                log.info("Video asset already READY [refId={}], skipping", refId);
                deleteQuietly(inputPath);
                return;
            }

            if (asset.getStatus() == VideoAssetStatus.PROCESSING) {
                long elapsed = ChronoUnit.SECONDS.between(asset.getUpdatedAt(), OffsetDateTime.now());
                if (elapsed < properties.getProcess().getTimeoutSeconds()) {
                    log.info("Video asset already PROCESSING [refId={}] {}s ago, skipping", refId, elapsed);
                    deleteQuietly(inputPath);
                    return;
                }
                log.warn("Video asset PROCESSING timed out [refId={}] {}s ago, retrying", refId, elapsed);
            }

            videoAssetRepository.updateStatus(asset.getId(), VideoAssetStatus.UPLOADED);
        } else {
            videoAssetRepository.create(refId, teamId);
        }

        VideoAsset asset = videoAssetRepository.findByRefId(refId)
                .orElseThrow(() -> new IllegalStateException("VideoAsset not found after create for refId=" + refId));

        UUID assetId = asset.getId();
        CompletableFuture.runAsync(() -> processVideo(assetId, refId, inputPath), persistExecutor);
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public void persistManual(UUID teamId, MultipartFile file) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Video persistence is disabled (VIDEO_PERSIST_ENABLED=false)");
        }
        String refId = "manual-" + teamId;
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.mp4";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".mp4";
        Path tempPath = Paths.get(tempDir, refId + ext);
        try {
            Files.createDirectories(tempPath.getParent());
            file.transferTo(tempPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save uploaded file for teamId=" + teamId, e);
        }

        Optional<VideoAsset> existing = videoAssetRepository.findByRefId(refId);
        if (existing.isPresent()) {
            VideoAssetStatus status = existing.get().getStatus();
            if (status == VideoAssetStatus.READY || status == VideoAssetStatus.PROCESSING) {
                videoAssetRepository.updateStatus(existing.get().getId(), VideoAssetStatus.UPLOADED);
            }
        }

        persistAsync(refId, teamId, tempPath);
    }

    public Optional<VideoAsset> findByRefId(String refId) {
        return videoAssetRepository.findByRefId(refId);
    }

    public List<VideoAsset> findAll() {
        return videoAssetRepository.findAll();
    }

    public List<VideoAsset> findByTeamId(UUID teamId) {
        return videoAssetRepository.findByTeamId(teamId);
    }

    public String resolveUrl(VideoAsset asset) {
        if (asset.getPlaybackKey() == null) return null;
        if (properties.isPublicBucket()) {
            return videoStoragePort.getPublicUrl(properties.getBucket(), asset.getPlaybackKey());
        }
        return videoStoragePort.getSignedUrl(properties.getBucket(), asset.getPlaybackKey(),
                properties.getSignedUrlExpireSeconds());
    }

    private void processVideo(UUID assetId, String refId, Path inputPath) {
        Path outputPath = Paths.get(tempDir, refId + "_playback_540p.mp4");
        try {
            videoAssetRepository.updateStatus(assetId, VideoAssetStatus.PROCESSING);
            log.info("Video processing started [refId={}] input={}", refId, inputPath.getFileName());

            TranscodeResult result = transcodePort.transcode(
                    inputPath, outputPath,
                    properties.getMaxHeight(),
                    properties.getTargetFps(),
                    properties.getCrf(),
                    properties.getPreset(),
                    properties.getAudioBitrate()
            );

            String storageKey = "videos/" + refId + "/playback_540p.mp4";
            videoStoragePort.upload(properties.getBucket(), storageKey, outputPath, "video/mp4");

            videoAssetRepository.markReady(
                    assetId,
                    storageKey,
                    null,
                    result.durationMs(),
                    result.width(),
                    result.height(),
                    result.fps(),
                    result.sizeBytes()
            );

            log.info("Video persisted [refId={}] key={} duration={}s size={}MB",
                    refId, storageKey,
                    result.durationMs() / 1000,
                    result.sizeBytes() / (1024 * 1024));

        } catch (Exception e) {
            log.error("Video persistence failed [refId={}]: {}", refId, e.getMessage());
            videoAssetRepository.markFailed(assetId, e.getMessage());
        } finally {
            deleteQuietly(inputPath);
            deleteQuietly(outputPath);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete temp file [{}]: {}", path, e.getMessage());
        }
    }
}
