package com.yellowleaf.coulson.infrastructure.persistence.repository;

import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamMemberEntity;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringTeamMemberJpaRepository extends JpaRepository<TeamMemberEntity, TeamMemberId> {

    List<TeamMemberEntity> findAllById_TeamId(UUID teamId);
}
