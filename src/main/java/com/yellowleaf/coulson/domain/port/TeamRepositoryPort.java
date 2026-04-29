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

    TeamMember addMember(UUID teamId, UUID participantId);

    Optional<TeamMember> findMember(UUID teamId, UUID participantId);

    List<TeamMember> findAllMembers();

    List<TeamMember> findMembersByTeam(UUID teamId);

    List<TeamMember> findMembersByParticipant(UUID participantId);

    void removeMember(UUID teamId, UUID participantId);

    Team updatePresentationAudio(UUID id, String presentationAudioKey, double durationSeconds);
}
