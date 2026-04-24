package com.yellowleaf.coulson.infrastructure.persistence.repository;

import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringTeamJpaRepository extends JpaRepository<TeamEntity, UUID> {
}
