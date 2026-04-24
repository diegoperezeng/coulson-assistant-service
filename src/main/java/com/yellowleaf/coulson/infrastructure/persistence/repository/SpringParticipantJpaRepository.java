package com.yellowleaf.coulson.infrastructure.persistence.repository;

import com.yellowleaf.coulson.infrastructure.persistence.entity.ParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringParticipantJpaRepository extends JpaRepository<ParticipantEntity, UUID> {
}
