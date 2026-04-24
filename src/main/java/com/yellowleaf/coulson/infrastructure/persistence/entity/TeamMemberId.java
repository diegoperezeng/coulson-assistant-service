package com.yellowleaf.coulson.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TeamMemberId implements Serializable {

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "participant_id")
    private UUID participantId;

    public TeamMemberId() {}

    public TeamMemberId(UUID teamId, UUID participantId) {
        this.teamId = teamId;
        this.participantId = participantId;
    }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public UUID getParticipantId() { return participantId; }
    public void setParticipantId(UUID participantId) { this.participantId = participantId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TeamMemberId that)) return false;
        return Objects.equals(teamId, that.teamId) && Objects.equals(participantId, that.participantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, participantId);
    }
}
