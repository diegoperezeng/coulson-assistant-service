package com.yellowleaf.coulson.interfaces.rest.dto;

import com.yellowleaf.coulson.domain.model.Participant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Participant data")
public class ParticipantResponse {

    @Schema(description = "Participant ID")
    private final UUID id;

    @Schema(description = "Participant name", example = "Matheus")
    private final String name;

    @Schema(description = "Job role", example = "Engenheiro de Software")
    private final String role;

    @Schema(description = "Voice sample storage key (null if not provided)")
    private final String voiceSampleKey;

    @Schema(description = "Registration date")
    private final OffsetDateTime createdAt;

    public ParticipantResponse(UUID id, String name, String role, String voiceSampleKey, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.voiceSampleKey = voiceSampleKey;
        this.createdAt = createdAt;
    }

    public static ParticipantResponse from(Participant p) {
        return new ParticipantResponse(p.getId(), p.getName(), p.getRole(), p.getVoiceSampleKey(), p.getCreatedAt());
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getVoiceSampleKey() { return voiceSampleKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
