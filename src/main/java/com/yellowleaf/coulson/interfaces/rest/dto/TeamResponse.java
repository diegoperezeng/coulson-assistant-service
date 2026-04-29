package com.yellowleaf.coulson.interfaces.rest.dto;

import com.yellowleaf.coulson.domain.model.Team;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Team data")
public class TeamResponse {

    @Schema(description = "Team ID", example = "a3f1c2d4-89ab-4321-bcde-0123456789ab")
    private final UUID id;

    @Schema(description = "Team name", example = "Time de Produto")
    private final String name;

    @Schema(description = "Date the team was created")
    private final OffsetDateTime createdAt;

    @Schema(description = "List of members (only present on GET by ID)")
    private final List<TeamMemberResponse> members;

    @Schema(description = "Presentation audio storage key (null if not generated)")
    private final String presentationAudioKey;

    @Schema(description = "Duration in seconds of the team presentation audio")
    private final Double presentationAudioDurationSeconds;

    public TeamResponse(UUID id, String name, OffsetDateTime createdAt, List<TeamMemberResponse> members,
                        String presentationAudioKey, Double presentationAudioDurationSeconds) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.members = members;
        this.presentationAudioKey = presentationAudioKey;
        this.presentationAudioDurationSeconds = presentationAudioDurationSeconds;
    }

    public static TeamResponse from(Team team) {
        List<TeamMemberResponse> memberResponses = team.getMembers().stream()
                .map(TeamMemberResponse::from)
                .toList();
        return new TeamResponse(team.getId(), team.getName(), team.getCreatedAt(), memberResponses,
                team.getPresentationAudioKey(), team.getPresentationAudioDurationSeconds());
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<TeamMemberResponse> getMembers() { return members; }
    public String getPresentationAudioKey() { return presentationAudioKey; }
    public Double getPresentationAudioDurationSeconds() { return presentationAudioDurationSeconds; }
}
