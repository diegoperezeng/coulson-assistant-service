package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.VideoAsset;
import com.yellowleaf.coulson.domain.model.VideoAssetStatus;
import com.yellowleaf.coulson.domain.port.VideoAssetRepositoryPort;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TeamEntity;
import com.yellowleaf.coulson.infrastructure.persistence.entity.VideoAssetEntity;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringTeamJpaRepository;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringVideoAssetJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaVideoAssetRepositoryAdapter implements VideoAssetRepositoryPort {

    private final SpringVideoAssetJpaRepository repository;
    private final SpringTeamJpaRepository teamRepository;

    public JpaVideoAssetRepositoryAdapter(SpringVideoAssetJpaRepository repository,
                                          SpringTeamJpaRepository teamRepository) {
        this.repository = repository;
        this.teamRepository = teamRepository;
    }

    @Override
    @Transactional
    public VideoAsset create(String refId, UUID teamId) {
        TeamEntity team = teamRepository.getReferenceById(teamId);
        VideoAssetEntity entity = new VideoAssetEntity();
        entity.setRefId(refId);
        entity.setTeam(team);
        entity.setStatus(VideoAssetStatus.UPLOADED);
        OffsetDateTime now = OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoAsset> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VideoAsset> findByRefId(String refId) {
        return repository.findByRefId(refId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VideoAsset> findByTeamId(UUID teamId) {
        return repository.findByTeam_Id(teamId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, VideoAssetStatus status) {
        repository.updateStatus(id, status, OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void markReady(UUID id, String playbackKey, String hlsMasterKey,
                          Long durationMs, Integer width, Integer height, Double fps, Long playbackSizeBytes) {
        repository.markReady(id, playbackKey, hlsMasterKey, durationMs, width, height, fps, playbackSizeBytes, OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void markFailed(UUID id, String errorMessage) {
        repository.markFailed(id, errorMessage, OffsetDateTime.now());
    }

    private VideoAsset toDomain(VideoAssetEntity e) {
        return new VideoAsset(
                e.getId(), e.getRefId(), e.getTeamId(), e.getTeamName(), e.getStatus(),
                e.getPlaybackKey(), e.getHlsMasterKey(),
                e.getDurationMs(), e.getWidth(), e.getHeight(), e.getFps(),
                e.getPlaybackSizeBytes(), e.getErrorMessage(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
