package com.yellowleaf.coulson.application.result;

import com.yellowleaf.coulson.domain.model.TranscriptionSegment;

import java.util.List;

public class TranscriptionResult {

    private final String videoId;
    private final String transcription;
    private final int totalChunks;
    private final List<TranscriptionSegment> segments;

    public TranscriptionResult(String videoId, String transcription, int totalChunks, List<TranscriptionSegment> segments) {
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

    public List<TranscriptionSegment> getSegments() {
        return segments;
    }
}
