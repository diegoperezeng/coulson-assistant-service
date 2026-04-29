package com.yellowleaf.coulson.infrastructure.persistence.repository;

import com.yellowleaf.coulson.domain.model.TranscriptionJobStatus;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TranscriptionJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SpringTranscriptionJobJpaRepository extends JpaRepository<TranscriptionJobEntity, UUID> {

    Optional<TranscriptionJobEntity> findByFileHashAndStatus(String fileHash, TranscriptionJobStatus status);

    @Modifying
    @Query("UPDATE TranscriptionJobEntity j SET j.status = :status, j.result = :result, j.updatedAt = :now WHERE j.id = :id")
    void markDone(
            @Param("id") UUID id,
            @Param("status") TranscriptionJobStatus status,
            @Param("result") String result,
            @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE TranscriptionJobEntity j SET j.status = :status, j.error = :error, j.updatedAt = :now WHERE j.id = :id")
    void markFailed(
            @Param("id") UUID id,
            @Param("status") TranscriptionJobStatus status,
            @Param("error") String error,
            @Param("now") OffsetDateTime now);
}
