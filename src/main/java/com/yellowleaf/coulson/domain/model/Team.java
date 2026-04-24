package com.yellowleaf.coulson.domain.model;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Team {

    private final UUID id;
    private final String name;
    private final OffsetDateTime createdAt;
    private final List<TeamMember> members;

    public Team(UUID id, String name, OffsetDateTime createdAt, List<TeamMember> members) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.members = members != null ? Collections.unmodifiableList(members) : Collections.emptyList();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<TeamMember> getMembers() { return members; }
}
