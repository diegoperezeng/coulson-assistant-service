package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.TranscriptionJob;
import com.yellowleaf.coulson.domain.model.TranscriptionJobStatus;
import com.yellowleaf.coulson.domain.port.TranscriptionJobRepositoryPort;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TranscriptionJobEntity;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringTranscriptionJobJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public class JpaTranscriptionJobRepositoryAdapter implements TranscriptionJobRepositoryPort {

    private final SpringTranscriptionJobJpaRepository repository;

    public JpaTranscriptionJobRepositoryAdapter(SpringTranscriptionJobJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TranscriptionJob create(String videoName) {
        TranscriptionJobEntity entity = new TranscriptionJobEntity();
        entity.setStatus(TranscriptionJobStatus.PROCESSING);
        entity.setVideoName(videoName);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return toJob(repository.save(entity));
    }

    @Override
    @Transactional
    public void markDone(UUID jobId, String resultJson) {
        repository.markDone(jobId, TranscriptionJobStatus.DONE, resultJson, OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void markFailed(UUID jobId, String error) {
        repository.markFailed(jobId, TranscriptionJobStatus.FAILED, error, OffsetDateTime.now());
    }

    @Override
    public Optional<TranscriptionJob> findById(UUID jobId) {
        return repository.findById(jobId).map(this::toJob);
    }

    private TranscriptionJob toJob(TranscriptionJobEntity e) {
        return new TranscriptionJob(
                e.getId(),
                e.getStatus(),
                e.getVideoName(),
                e.getError(),
                e.getResult(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
