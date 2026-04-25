package com.yellowleaf.coulson.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yellowleaf.coulson.application.result.TranscriptionResult;
import com.yellowleaf.coulson.domain.model.TranscriptionJob;
import com.yellowleaf.coulson.domain.model.Video;
import com.yellowleaf.coulson.domain.port.TranscriptionJobRepositoryPort;
import com.yellowleaf.coulson.domain.port.VideoStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SubmitTranscriptionJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmitTranscriptionJobUseCase.class);

    private final VideoStoragePort storagePort;
    private final TranscriptionJobRepositoryPort jobRepository;
    private final ProcessVideoUseCase processVideoUseCase;
    private final ObjectMapper objectMapper;
    private final Executor jobExecutor;

    public SubmitTranscriptionJobUseCase(
            VideoStoragePort storagePort,
            TranscriptionJobRepositoryPort jobRepository,
            ProcessVideoUseCase processVideoUseCase,
            ObjectMapper objectMapper,
            Executor jobExecutor) {
        this.storagePort = storagePort;
        this.jobRepository = jobRepository;
        this.processVideoUseCase = processVideoUseCase;
        this.objectMapper = objectMapper;
        this.jobExecutor = jobExecutor;
    }

    public TranscriptionJob submit(MultipartFile file) {
        Video video = storagePort.store(file);
        log.info("Video stored for async processing [videoId={}] file={}", video.getId(), video.getOriginalName());

        TranscriptionJob job = jobRepository.create(video.getOriginalName());
        log.info("Transcription job created [jobId={}]", job.getId());

        CompletableFuture.runAsync(() -> processAsync(job.getId(), video), jobExecutor);

        return job;
    }

    private void processAsync(java.util.UUID jobId, Video video) {
        log.info("Async transcription started [jobId={}] videoId={}", jobId, video.getId());
        try {
            TranscriptionResult result = processVideoUseCase.process(video);
            String resultJson = serialize(result);
            jobRepository.markDone(jobId, resultJson);
            log.info("Async transcription done [jobId={}]", jobId);
        } catch (Exception e) {
            log.error("Async transcription failed [jobId={}]: {}", jobId, e.getMessage());
            jobRepository.markFailed(jobId, e.getMessage());
        }
    }

    private String serialize(TranscriptionResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transcription result", e);
        }
    }
}
