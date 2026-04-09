package com.yellowleaf.coulson.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard error response body")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "502")
    private final int status;

    @Schema(description = "Short error label", example = "Bad Gateway")
    private final String error;

    @Schema(
            description = "Detailed message describing the cause of the error",
            example = "Failed to communicate with transcription webhook"
    )
    private final String message;

    @Schema(description = "Timestamp of the error in ISO-8601 format", example = "2026-04-07T14:32:10.123Z")
    private final Instant timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
