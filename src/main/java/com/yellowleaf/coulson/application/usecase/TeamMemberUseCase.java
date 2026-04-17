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

    public TeamMember add(UUID teamId, String name) {
        return teamRepositoryPort.saveMember(teamId, name);
    }

    public TeamMember findById(UUID teamId, UUID memberId) {
        return teamRepositoryPort.findMemberById(teamId, memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found: " + memberId));
    }

    public List<TeamMember> findAllByTeam(UUID teamId) {
        return teamRepositoryPort.findMembersByTeam(teamId);
    }

    public TeamMember update(UUID memberId, String name) {
        return teamRepositoryPort.updateMember(memberId, name);
    }

    public void delete(UUID memberId) {
        teamRepositoryPort.deleteMember(memberId);
    }
}
