package com.yellowleaf.coulson.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of the video transcription process")
public class TranscriptionResponse {

    @Schema(
            description = "Unique identifier generated for the processed video",
            example = "a3f1c2d4-89ab-4321-bcde-0123456789ab"
    )
    private final String videoId;

    @Schema(
            description = "Full transcription text joined from all segments.",
            example = "Hello, welcome to this tutorial. Today we will cover the basics of Spring Boot..."
    )
    private final String transcription;

    @Schema(
            description = "Number of video chunks the file was divided into. Will be 1 if the file was under 200 MB.",
            example = "3"
    )
    private final int totalChunks;

    @Schema(description = "List of speaker segments with timestamps, ordered chronologically.")
    private final List<TranscriptionSegmentDto> segments;

    public TranscriptionResponse(String videoId, String transcription, int totalChunks, List<TranscriptionSegmentDto> segments) {
        this.videoId = videoId;
        this.transcription = transcription;
        this.totalChunks = totalChunks;
        this.segments = segments;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTranscription() {
        return transcription;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public List<TranscriptionSegmentDto> getSegments() {
        return segments;
    }
}
