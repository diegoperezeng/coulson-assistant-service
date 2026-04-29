package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.TranscriptionJob;

import java.util.Optional;
import java.util.UUID;

public interface TranscriptionJobRepositoryPort {

    TranscriptionJob create(String videoName, String fileHash);

    void markDone(UUID jobId, String resultJson);

    void markFailed(UUID jobId, String error);

    Optional<TranscriptionJob> findById(UUID jobId);

    Optional<TranscriptionJob> findDoneByFileHash(String fileHash);
}
