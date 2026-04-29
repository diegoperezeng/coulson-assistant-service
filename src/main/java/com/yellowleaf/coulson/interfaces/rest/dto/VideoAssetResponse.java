package com.yellowleaf.coulson.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yellowleaf.coulson.domain.model.VideoAssetStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoAssetResponse {

    private UUID id;
    private String refId;
    private UUID teamId;
    private String teamName;
    private VideoAssetStatus status;
    private String playbackUrl;
    private String hlsMasterUrl;
    private Long durationMs;
    private Integer width;
    private Integer height;
    private Double fps;
    private Long playbackSizeBytes;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public VideoAssetResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public VideoAssetStatus getStatus() { return status; }
    public void setStatus(VideoAssetStatus status) { this.status = status; }

    public String getPlaybackUrl() { return playbackUrl; }
    public void setPlaybackUrl(String playbackUrl) { this.playbackUrl = playbackUrl; }

    public String getHlsMasterUrl() { return hlsMasterUrl; }
    public void setHlsMasterUrl(String hlsMasterUrl) { this.hlsMasterUrl = hlsMasterUrl; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Double getFps() { return fps; }
    public void setFps(Double fps) { this.fps = fps; }

    public Long getPlaybackSizeBytes() { return playbackSizeBytes; }
    public void setPlaybackSizeBytes(Long playbackSizeBytes) { this.playbackSizeBytes = playbackSizeBytes; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
