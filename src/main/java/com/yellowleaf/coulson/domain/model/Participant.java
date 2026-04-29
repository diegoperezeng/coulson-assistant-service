package com.yellowleaf.coulson.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Participant {

    private final UUID id;
    private final String name;
    private final String role;
    private final String voiceSampleKey;
    private final String voiceId;
    private final String presentationAudioKey;
    private final OffsetDateTime createdAt;

    public Participant(UUID id, String name, String role, String voiceSampleKey,
                       String voiceId, String presentationAudioKey, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.voiceSampleKey = voiceSampleKey;
        this.voiceId = voiceId;
        this.presentationAudioKey = presentationAudioKey;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getVoiceSampleKey() { return voiceSampleKey; }
    public String getVoiceId() { return voiceId; }
    public String getPresentationAudioKey() { return presentationAudioKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
