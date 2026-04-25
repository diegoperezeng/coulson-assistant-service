package com.yellowleaf.coulson.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TranscriptionJob {

    private final UUID id;
    private final TranscriptionJobStatus status;
    private final String videoName;
    private final String error;
    private final String resultJson;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public TranscriptionJob(
            UUID id,
            TranscriptionJobStatus status,
            String videoName,
            String error,
            String resultJson,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.videoName = videoName;
        this.error = error;
        this.resultJson = resultJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public TranscriptionJobStatus getStatus() { return status; }
    public String getVideoName() { return videoName; }
    public String getError() { return error; }
    public String getResultJson() { return resultJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
