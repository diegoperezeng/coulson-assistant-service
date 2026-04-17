package com.yellowleaf.coulson.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TeamMember {

    private final UUID id;
    private final UUID teamId;
    private final String name;
    private final OffsetDateTime createdAt;

    public TeamMember(UUID id, UUID teamId, String name, OffsetDateTime createdAt) {
        this.id = id;
        this.teamId = teamId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getTeamId() { return teamId; }
    public String getName() { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
