package com.yellowleaf.coulson.application.usecase;

import com.yellowleaf.coulson.domain.model.Team;
import com.yellowleaf.coulson.domain.port.TeamRepositoryPort;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public class TeamUseCase {

    private final TeamRepositoryPort teamRepositoryPort;

    public TeamUseCase(TeamRepositoryPort teamRepositoryPort) {
        this.teamRepositoryPort = teamRepositoryPort;
    }

    public Team create(String name) {
        return teamRepositoryPort.saveTeam(name);
    }

    public Team findById(UUID id) {
        return teamRepositoryPort.findTeamById(id)
                .orElseThrow(() -> new NoSuchElementException("Team not found: " + id));
    }

    public List<Team> findAll() {
        return teamRepositoryPort.findAllTeams();
    }

    public void delete(UUID id) {
        Optional<Team> team = teamRepositoryPort.findTeamById(id);
        if (team.isEmpty()) {
            throw new NoSuchElementException("Team not found: " + id);
        }
        teamRepositoryPort.deleteTeam(id);
    }
}
