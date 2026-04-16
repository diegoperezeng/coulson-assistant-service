package com.yellowleaf.coulson.infrastructure.persistence.repository;

import com.yellowleaf.coulson.infrastructure.persistence.entity.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SpringMeetingJpaRepository extends JpaRepository<MeetingEntity, UUID> {

    @Modifying
    @Query("UPDATE MeetingEntity m SET m.embeddedAt = :now WHERE m.id = :id")
    void markAsEmbedded(@Param("id") UUID id, @Param("now") OffsetDateTime now);
}
