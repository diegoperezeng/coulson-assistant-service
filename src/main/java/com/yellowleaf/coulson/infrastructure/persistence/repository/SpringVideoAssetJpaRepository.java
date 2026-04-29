package com.yellowleaf.coulson.infrastructure.persistence.repository;

import com.yellowleaf.coulson.domain.model.VideoAssetStatus;
import com.yellowleaf.coulson.infrastructure.persistence.entity.VideoAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringVideoAssetJpaRepository extends JpaRepository<VideoAssetEntity, UUID> {

    Optional<VideoAssetEntity> findByRefId(String refId);

    List<VideoAssetEntity> findByTeam_Id(UUID teamId);

    @Modifying
    @Transactional
    @Query("UPDATE VideoAssetEntity e SET e.status = :status, e.updatedAt = :now WHERE e.id = :id")
    void updateStatus(@Param("id") UUID id,
                      @Param("status") VideoAssetStatus status,
                      @Param("now") OffsetDateTime now);

    @Modifying
    @Transactional
    @Query("""
            UPDATE VideoAssetEntity e SET
              e.status = 'READY',
              e.playbackKey = :playbackKey,
              e.hlsMasterKey = :hlsMasterKey,
              e.durationMs = :durationMs,
              e.width = :width,
              e.height = :height,
              e.fps = :fps,
              e.playbackSizeBytes = :playbackSizeBytes,
              e.updatedAt = :now
            WHERE e.id = :id
            """)
    void markReady(@Param("id") UUID id,
                   @Param("playbackKey") String playbackKey,
                   @Param("hlsMasterKey") String hlsMasterKey,
                   @Param("durationMs") Long durationMs,
                   @Param("width") Integer width,
                   @Param("height") Integer height,
                   @Param("fps") Double fps,
                   @Param("playbackSizeBytes") Long playbackSizeBytes,
                   @Param("now") OffsetDateTime now);

    @Modifying
    @Transactional
    @Query("""
            UPDATE VideoAssetEntity e SET
              e.status = 'FAILED',
              e.errorMessage = :errorMessage,
              e.updatedAt = :now
            WHERE e.id = :id
            """)
    void markFailed(@Param("id") UUID id,
                    @Param("errorMessage") String errorMessage,
                    @Param("now") OffsetDateTime now);
}
