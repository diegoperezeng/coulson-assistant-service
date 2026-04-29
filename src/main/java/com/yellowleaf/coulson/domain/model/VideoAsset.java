package com.yellowleaf.coulson.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class VideoAsset {

    private UUID id;
    private String refId;
    private UUID teamId;
    private String teamName;
    private VideoAssetStatus status;
    private String playbackKey;
    private String hlsMasterKey;
    private Long durationMs;
    private Integer width;
    private Integer height;
    private Double fps;
    private Long playbackSizeBytes;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public VideoAsset() {}

    public VideoAsset(UUID id, String refId, UUID teamId, String teamName, VideoAssetStatus status,
                      String playbackKey, String hlsMasterKey,
                      Long durationMs, Integer width, Integer height, Double fps,
                      Long playbackSizeBytes, String errorMessage,
                      OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.refId = refId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.status = status;
        this.playbackKey = playbackKey;
        this.hlsMasterKey = hlsMasterKey;
        this.durationMs = durationMs;
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.playbackSizeBytes = playbackSizeBytes;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getRefId() { return refId; }
    public UUID getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public VideoAssetStatus getStatus() { return status; }
    public String getPlaybackKey() { return playbackKey; }
    public String getHlsMasterKey() { return hlsMasterKey; }
    public Long getDurationMs() { return durationMs; }
    public Integer getWidth() { return width; }
    public Integer getHeight() { return height; }
    public Double getFps() { return fps; }
    public Long getPlaybackSizeBytes() { return playbackSizeBytes; }
    public String getErrorMessage() { return errorMessage; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
