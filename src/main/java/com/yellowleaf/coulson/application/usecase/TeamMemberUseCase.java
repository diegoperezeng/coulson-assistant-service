package com.yellowleaf.coulson.application.usecase;

import com.yellowleaf.coulson.domain.model.TeamMember;
import com.yellowleaf.coulson.domain.port.TeamRepositoryPort;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public class TeamMemberUseCase {

    private final TeamRepositoryPort teamRepositoryPort;

    public TeamMemberUseCase(TeamRepositoryPort teamRepositoryPort) {
        this.teamRepositoryPort = teamRepositoryPort;
    }

    public TeamMember add(UUID teamId, UUID participantId) {
        return teamRepositoryPort.addMember(teamId, participantId);
    }

    public TeamMember findById(UUID teamId, UUID participantId) {
        return teamRepositoryPort.findMember(teamId, participantId)
                .orElseThrow(() -> new NoSuchElementException("Member not found in team"));
    }

    public List<TeamMember> findAll() {
        return teamRepositoryPort.findAllMembers();
    }

    public List<TeamMember> findAllByTeam(UUID teamId) {
        return teamRepositoryPort.findMembersByTeam(teamId);
    }

    public void remove(UUID teamId, UUID participantId) {
        teamRepositoryPort.removeMember(teamId, participantId);
    }
}
