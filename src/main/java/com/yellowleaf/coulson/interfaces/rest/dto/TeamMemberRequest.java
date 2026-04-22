package com.yellowleaf.coulson.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request payload to add a participant to a team")
public class TeamMemberRequest {

    @NotNull
    @Schema(description = "Participant ID", example = "b2f3c1d4-89ab-4321-bcde-0123456789ab")
    private UUID participantId;

    public UUID getParticipantId() { return participantId; }
    public void setParticipantId(UUID participantId) { this.participantId = participantId; }
}
