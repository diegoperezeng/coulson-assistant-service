package com.yellowleaf.coulson.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TeamMember {

    private final UUID teamId;
    private final Participant participant;
    private final OffsetDateTime addedAt;

    public TeamMember(UUID teamId, Participant participant, OffsetDateTime addedAt) {
        this.teamId = teamId;
        this.participant = participant;
        this.addedAt = addedAt;
    }

    public UUID getTeamId() { return teamId; }
    public Participant getParticipant() { return participant; }
    public OffsetDateTime getAddedAt() { return addedAt; }
}
