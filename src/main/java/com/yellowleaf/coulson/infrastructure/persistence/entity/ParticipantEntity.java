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
@Table(name = "participants")
public class ParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String role;

    @Column(name = "voice_sample_key", length = 1000)
    private String voiceSampleKey;

    @Column(name = "voice_id", length = 255)
    private String voiceId;

    @Column(name = "presentation_audio_key", length = 1000)
    private String presentationAudioKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getVoiceSampleKey() { return voiceSampleKey; }
    public void setVoiceSampleKey(String voiceSampleKey) { this.voiceSampleKey = voiceSampleKey; }

    public String getVoiceId() { return voiceId; }
    public void setVoiceId(String voiceId) { this.voiceId = voiceId; }

    public String getPresentationAudioKey() { return presentationAudioKey; }
    public void setPresentationAudioKey(String presentationAudioKey) { this.presentationAudioKey = presentationAudioKey; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
