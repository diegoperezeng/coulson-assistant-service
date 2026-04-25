package com.yellowleaf.coulson.application.usecase;

import com.yellowleaf.coulson.domain.model.TranscriptionJob;
import com.yellowleaf.coulson.domain.port.TranscriptionJobRepositoryPort;

import java.util.NoSuchElementException;
import java.util.UUID;

public class GetTranscriptionJobUseCase {

    private final TranscriptionJobRepositoryPort jobRepository;

    public GetTranscriptionJobUseCase(TranscriptionJobRepositoryPort jobRepository) {
        this.jobRepository = jobRepository;
    }

    public TranscriptionJob get(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("Transcription job not found: " + jobId));
    }
}
