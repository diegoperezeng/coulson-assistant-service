package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.Team;
import com.yellowleaf.coulson.domain.model.TeamMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepositoryPort {

    Team saveTeam(String name);

    Optional<Team> findTeamById(UUID id);

    List<Team> findAllTeams();

    void deleteTeam(UUID id);

    TeamMember saveMember(UUID teamId, String name);

    Optional<TeamMember> findMemberById(UUID teamId, UUID memberId);

    List<TeamMember> findMembersByTeam(UUID teamId);

    TeamMember updateMember(UUID memberId, String name);

    void deleteMember(UUID memberId);
}
