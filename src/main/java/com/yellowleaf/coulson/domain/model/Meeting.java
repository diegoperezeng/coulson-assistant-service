package com.yellowleaf.coulson.domain.model;

import java.util.Collections;
import java.util.List;

public class Meeting {

    private final String id;
    private final String title;
    private final double durationSeconds;
    private final String fullText;
    private final List<TranscriptionSegment> segments;

    public Meeting(String id, String title, double durationSeconds, String fullText, List<TranscriptionSegment> segments) {
        this.id = id;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.fullText = fullText;
        this.segments = segments != null ? Collections.unmodifiableList(segments) : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public String getFullText() {
        return fullText;
    }

    public List<TranscriptionSegment> getSegments() {
        return segments;
    }
}
