package com.yellowleaf.coulson.infrastructure.persistence.entity;

import com.yellowleaf.coulson.domain.model.VideoAssetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_asset")
public class VideoAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ref_id", nullable = false, unique = true, length = 255)
    private String refId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VideoAssetStatus status;

    @Column(name = "playback_key", length = 1000)
    private String playbackKey;

    @Column(name = "hls_master_key", length = 1000)
    private String hlsMasterKey;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "fps")
    private Double fps;

    @Column(name = "playback_size_bytes")
    private Long playbackSizeBytes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public TeamEntity getTeam() { return team; }
    public void setTeam(TeamEntity team) { this.team = team; }

    public UUID getTeamId() { return team != null ? team.getId() : null; }
    public String getTeamName() { return team != null ? team.getName() : null; }

    public VideoAssetStatus getStatus() { return status; }
    public void setStatus(VideoAssetStatus status) { this.status = status; }

    public String getPlaybackKey() { return playbackKey; }
    public void setPlaybackKey(String playbackKey) { this.playbackKey = playbackKey; }

    public String getHlsMasterKey() { return hlsMasterKey; }
    public void setHlsMasterKey(String hlsMasterKey) { this.hlsMasterKey = hlsMasterKey; }

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
