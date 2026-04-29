package com.yellowleaf.coulson.interfaces.rest.dto;

import com.yellowleaf.coulson.domain.model.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Team member data")
public class TeamMemberResponse {

    @Schema(description = "Team ID")
    private final UUID teamId;

    @Schema(description = "Team name")
    private final String teamName;

    @Schema(description = "Participant data")
    private final ParticipantResponse participant;

    @Schema(description = "Date the participant was added to the team")
    private final OffsetDateTime addedAt;

    public TeamMemberResponse(UUID teamId, String teamName, ParticipantResponse participant, OffsetDateTime addedAt) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.participant = participant;
        this.addedAt = addedAt;
    }

    public static TeamMemberResponse from(TeamMember member) {
        return new TeamMemberResponse(
                member.getTeamId(),
                member.getTeamName(),
                ParticipantResponse.from(member.getParticipant()),
                member.getAddedAt());
    }

    public UUID getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public ParticipantResponse getParticipant() { return participant; }
    public OffsetDateTime getAddedAt() { return addedAt; }
}
