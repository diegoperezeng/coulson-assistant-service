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
    private final String presentationAudioKey;
    private final Double presentationAudioDurationSeconds;

    public Team(UUID id, String name, OffsetDateTime createdAt, List<TeamMember> members,
                String presentationAudioKey, Double presentationAudioDurationSeconds) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.members = members != null ? Collections.unmodifiableList(members) : Collections.emptyList();
        this.presentationAudioKey = presentationAudioKey;
        this.presentationAudioDurationSeconds = presentationAudioDurationSeconds;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<TeamMember> getMembers() { return members; }
    public String getPresentationAudioKey() { return presentationAudioKey; }
    public Double getPresentationAudioDurationSeconds() { return presentationAudioDurationSeconds; }
}
