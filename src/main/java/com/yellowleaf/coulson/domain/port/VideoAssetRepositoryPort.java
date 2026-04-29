package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.VideoAsset;
import com.yellowleaf.coulson.domain.model.VideoAssetStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoAssetRepositoryPort {

    VideoAsset create(String refId, UUID teamId);

    List<VideoAsset> findAll();

    Optional<VideoAsset> findByRefId(String refId);

    List<VideoAsset> findByTeamId(UUID teamId);

    void updateStatus(UUID id, VideoAssetStatus status);

    void markReady(UUID id, String playbackKey, String hlsMasterKey,
                   Long durationMs, Integer width, Integer height, Double fps, Long playbackSizeBytes);

    void markFailed(UUID id, String errorMessage);
}
