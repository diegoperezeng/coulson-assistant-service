package com.yellowleaf.coulson.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meetings")
public class MeetingEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "duration_sec", precision = 10, scale = 2)
    private BigDecimal durationSec;

    @Column(name = "video_storage_key", length = 1000)
    private String videoStorageKey;

    @Column(name = "full_text", nullable = false, columnDefinition = "TEXT")
    private String fullText;

    @Column(name = "embedded_at")
    private OffsetDateTime embeddedAt;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TranscriptionSegmentEntity> segments = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }

    public BigDecimal getDurationSec() { return durationSec; }
    public void setDurationSec(BigDecimal durationSec) { this.durationSec = durationSec; }

    public String getVideoStorageKey() { return videoStorageKey; }
    public void setVideoStorageKey(String videoStorageKey) { this.videoStorageKey = videoStorageKey; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public OffsetDateTime getEmbeddedAt() { return embeddedAt; }
    public void setEmbeddedAt(OffsetDateTime embeddedAt) { this.embeddedAt = embeddedAt; }

    public List<TranscriptionSegmentEntity> getSegments() { return segments; }
    public void setSegments(List<TranscriptionSegmentEntity> segments) { this.segments = segments; }
}
