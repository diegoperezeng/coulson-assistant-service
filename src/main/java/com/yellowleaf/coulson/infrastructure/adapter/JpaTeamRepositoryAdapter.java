package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.Team;
import com.yellowleaf.coulson.domain.model.TeamMember;
import com.yellowleaf.coulson.domain.port.TeamRepositoryPort;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamEntity;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamMemberEntity;
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

    public JpaTeamRepositoryAdapter(
            SpringTeamJpaRepository teamRepository,
            SpringTeamMemberJpaRepository memberRepository) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public Team saveTeam(String name) {
        TeamEntity entity = new TeamEntity();
        entity.setName(name);
        entity.setCreatedAt(OffsetDateTime.now());
        TeamEntity saved = teamRepository.save(entity);
        return toTeam(saved);
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
    public TeamMember saveMember(UUID teamId, String name) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NoSuchElementException("Team not found: " + teamId));
        TeamMemberEntity entity = new TeamMemberEntity();
        entity.setTeam(team);
        entity.setName(name);
        entity.setCreatedAt(OffsetDateTime.now());
        return toMember(memberRepository.save(entity));
    }

    @Override
    public Optional<TeamMember> findMemberById(UUID teamId, UUID memberId) {
        return memberRepository.findByIdAndTeamId(memberId, teamId).map(this::toMember);
    }

    @Override
    public List<TeamMember> findMembersByTeam(UUID teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new NoSuchElementException("Team not found: " + teamId);
        }
        return memberRepository.findAllByTeamId(teamId).stream().map(this::toMember).toList();
    }

    @Override
    public TeamMember updateMember(UUID memberId, String name) {
        TeamMemberEntity entity = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found: " + memberId));
        entity.setName(name);
        return toMember(memberRepository.save(entity));
    }

    @Override
    public void deleteMember(UUID memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new NoSuchElementException("Member not found: " + memberId);
        }
        memberRepository.deleteById(memberId);
    }

    private Team toTeam(TeamEntity e) {
        return new Team(e.getId(), e.getName(), e.getCreatedAt(), List.of());
    }

    private Team toTeamWithMembers(TeamEntity e) {
        List<TeamMember> members = memberRepository.findAllByTeamId(e.getId())
                .stream().map(this::toMember).toList();
        return new Team(e.getId(), e.getName(), e.getCreatedAt(), members);
    }

    private TeamMember toMember(TeamMemberEntity e) {
        return new TeamMember(e.getId(), e.getTeam().getId(), e.getName(), e.getCreatedAt());
    }
}
