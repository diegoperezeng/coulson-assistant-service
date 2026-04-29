package com.yellowleaf.coulson.interfaces.rest.dto;

import com.yellowleaf.coulson.domain.model.Participant;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
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

    @Schema(description = "Teams the participant currently belongs to")
    private final List<TeamInfo> teams;

    public ParticipantResponse(UUID id, String name, String role, String voiceSampleKey,
                               OffsetDateTime createdAt, List<TeamInfo> teams) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.voiceSampleKey = voiceSampleKey;
        this.createdAt = createdAt;
        this.teams = teams != null ? teams : List.of();
    }

    public static ParticipantResponse from(Participant p) {
        return new ParticipantResponse(p.getId(), p.getName(), p.getRole(),
                p.getVoiceSampleKey(), p.getCreatedAt(), List.of());
    }

    public static ParticipantResponse from(Participant p, List<TeamInfo> teams) {
        return new ParticipantResponse(p.getId(), p.getName(), p.getRole(),
                p.getVoiceSampleKey(), p.getCreatedAt(), teams);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getVoiceSampleKey() { return voiceSampleKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<TeamInfo> getTeams() { return teams; }

    @Schema(description = "Basic team info")
    public static class TeamInfo {

        @Schema(description = "Team ID")
        private final UUID id;

        @Schema(description = "Team name")
        private final String name;

        public TeamInfo(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
    }
}
