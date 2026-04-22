package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.Participant;
import com.yellowleaf.coulson.domain.model.Team;
import com.yellowleaf.coulson.domain.model.TeamMember;
import com.yellowleaf.coulson.domain.port.TeamRepositoryPort;
import com.yellowleaf.coulson.infrastructure.persistence.entity.ParticipantEntity;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamEntity;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamMemberEntity;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamMemberId;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringParticipantJpaRepository;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringTeamJpaRepository;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringTeamMemberJpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public class JpaTeamRepositoryAdapter implements TeamRepositoryPort {

    private final SpringTeamJpaRepository teamRepository;
    private final SpringTeamMemberJpaRepository memberRepository;
    private final SpringParticipantJpaRepository participantRepository;

    public JpaTeamRepositoryAdapter(
            SpringTeamJpaRepository teamRepository,
            SpringTeamMemberJpaRepository memberRepository,
            SpringParticipantJpaRepository participantRepository) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    public Team saveTeam(String name) {
        TeamEntity entity = new TeamEntity();
        entity.setName(name);
        entity.setCreatedAt(OffsetDateTime.now());
        return toTeam(teamRepository.save(entity));
    }

    @Override
    public Optional<Team> findTeamById(UUID id) {
        return teamRepository.findById(id).map(this::toTeamWithMembers);
    }

    @Override
    public List<Team> findAllTeams() {
        return teamRepository.findAll().stream().map(this::toTeam).toList();
    }

    @Override
    public void deleteTeam(UUID id) {
        if (!teamRepository.existsById(id)) {
            throw new NoSuchElementException("Team not found: " + id);
        }
        teamRepository.deleteById(id);
    }

    @Override
    public TeamMember addMember(UUID teamId, UUID participantId) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Team not found: " + teamId));
        ParticipantEntity participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new NoSuchElementException("Participant not found: " + participantId));

        TeamMemberId pk = new TeamMemberId(teamId, participantId);
        if (memberRepository.existsById(pk)) {
            throw new IllegalStateException("Participant already belongs to this team");
        }

        TeamMemberEntity entity = new TeamMemberEntity();
        entity.setId(pk);
        entity.setTeam(team);
        entity.setParticipant(participant);
        entity.setAddedAt(OffsetDateTime.now());
        return toMember(memberRepository.save(entity));
    }

    @Override
    public Optional<TeamMember> findMember(UUID teamId, UUID participantId) {
        return memberRepository.findById(new TeamMemberId(teamId, participantId)).map(this::toMember);
    }

    @Override
    public List<TeamMember> findAllMembers() {
        return memberRepository.findAll().stream().map(this::toMember).toList();
    }

    @Override
    public List<TeamMember> findMembersByTeam(UUID teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new NoSuchElementException("Team not found: " + teamId);
        }
        return memberRepository.findAllById_TeamId(teamId).stream().map(this::toMember).toList();
    }

    @Override
    public void removeMember(UUID teamId, UUID participantId) {
        TeamMemberId pk = new TeamMemberId(teamId, participantId);
        if (!memberRepository.existsById(pk)) {
            throw new NoSuchElementException("Member not found in team");
        }
        memberRepository.deleteById(pk);
    }

    private Team toTeam(TeamEntity e) {
        return new Team(e.getId(), e.getName(), e.getCreatedAt(), List.of());
    }

    private Team toTeamWithMembers(TeamEntity e) {
        List<TeamMember> members = memberRepository.findAllById_TeamId(e.getId())
                .stream().map(this::toMember).toList();
        return new Team(e.getId(), e.getName(), e.getCreatedAt(), members);
    }

    private TeamMember toMember(TeamMemberEntity e) {
        return new TeamMember(e.getId().getTeamId(), toParticipant(e.getParticipant()), e.getAddedAt());
    }

    private Participant toParticipant(ParticipantEntity e) {
        return new Participant(e.getId(), e.getName(), e.getRole(), e.getVoiceSampleKey(), e.getCreatedAt());
    }
}
