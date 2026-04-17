package com.yellowleaf.coulson.infrastructure.persistence.repository;

import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringTeamMemberJpaRepository extends JpaRepository<TeamMemberEntity, UUID> {

    List<TeamMemberEntity> findAllByTeamId(UUID teamId);

    Optional<TeamMemberEntity> findByIdAndTeamId(UUID id, UUID teamId);
}
