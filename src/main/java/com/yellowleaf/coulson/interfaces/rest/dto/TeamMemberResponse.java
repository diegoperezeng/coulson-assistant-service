package com.yellowleaf.coulson.interfaces.rest.dto;

import com.yellowleaf.coulson.domain.model.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Team member data")
public class TeamMemberResponse {

    @Schema(description = "Member ID", example = "b2f3c1d4-89ab-4321-bcde-0123456789ab")
    private final UUID id;

    @Schema(description = "Team ID", example = "a3f1c2d4-89ab-4321-bcde-0123456789ab")
    private final UUID teamId;

    @Schema(description = "Member name", example = "Matheus")
    private final String name;

    @Schema(description = "Date the member was added")
    private final OffsetDateTime createdAt;

    public TeamMemberResponse(UUID id, UUID teamId, String name, OffsetDateTime createdAt) {
        this.id = id;
        this.teamId = teamId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(member.getId(), member.getTeamId(), member.getName(), member.getCreatedAt());
    }

    public UUID getId() { return id; }
    public UUID getTeamId() { return teamId; }
    public String getName() { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
