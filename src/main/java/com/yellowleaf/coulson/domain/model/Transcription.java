package com.yellowleaf.coulson.domain.model;

import java.util.Collections;
import java.util.List;

public class Transcription {

    private final int chunkIndex;
    private final String videoId;
    private final String text;
    private final List<TranscriptionSegment> segments;

    public Transcription(int chunkIndex, String videoId, String text, List<TranscriptionSegment> segments) {
        this.chunkIndex = chunkIndex;
        this.videoId = videoId;
        this.text = text;
        this.segments = segments != null ? Collections.unmodifiableList(segments) : Collections.emptyList();
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getText() {
        return text;
    }

    public List<TranscriptionSegment> getSegments() {
        return segments;
    }
}
