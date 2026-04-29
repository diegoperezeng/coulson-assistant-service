package com.yellowleaf.coulson.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class TeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "presentation_audio_key", length = 1000)
    private String presentationAudioKey;

    @Column(name = "presentation_audio_duration_seconds")
    private Double presentationAudioDurationSeconds;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getPresentationAudioKey() { return presentationAudioKey; }
    public void setPresentationAudioKey(String presentationAudioKey) { this.presentationAudioKey = presentationAudioKey; }

    public Double getPresentationAudioDurationSeconds() { return presentationAudioDurationSeconds; }
    public void setPresentationAudioDurationSeconds(Double presentationAudioDurationSeconds) { this.presentationAudioDurationSeconds = presentationAudioDurationSeconds; }
}
