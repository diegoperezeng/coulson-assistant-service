package com.yellowleaf.coulson.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Status and result of an async transcription job")
public class TranscriptionJobResponse {

    @Schema(description = "Unique identifier of the transcription job")
    private final UUID jobId;

    @Schema(description = "Current status of the job", allowableValues = {"PROCESSING", "DONE", "FAILED"})
    private final String status;

    @Schema(description = "Original name of the uploaded video file")
    private final String videoName;

    @Schema(description = "Error message, present only when status is FAILED")
    private final String error;

    @JsonRawValue
    @Schema(description = "Transcription result, present only when status is DONE")
    private final String result;

    @Schema(description = "Timestamp when the job was created")
    private final OffsetDateTime createdAt;

    @Schema(description = "Timestamp of the last status update")
    private final OffsetDateTime updatedAt;

    private TranscriptionJobResponse(Builder builder) {
        this.jobId = builder.jobId;
        this.status = builder.status;
        this.videoName = builder.videoName;
        this.error = builder.error;
        this.result = builder.result;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public UUID getJobId() { return jobId; }
    public String getStatus() { return status; }
    public String getVideoName() { return videoName; }
    public String getError() { return error; }
    public String getResult() { return result; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID jobId;
        private String status;
        private String videoName;
        private String error;
        private String result;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder jobId(UUID jobId) { this.jobId = jobId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder videoName(String videoName) { this.videoName = videoName; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder result(String result) { this.result = result; return this; }
        public Builder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public TranscriptionJobResponse build() {
            return new TranscriptionJobResponse(this);
        }
    }
}
