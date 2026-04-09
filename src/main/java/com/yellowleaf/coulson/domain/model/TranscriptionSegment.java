package com.yellowleaf.coulson.domain.model;

public class TranscriptionSegment {

    private final String speaker;
    private final double start;
    private final double end;
    private final String text;

    public TranscriptionSegment(String speaker, double start, double end, String text) {
        this.speaker = speaker;
        this.start = start;
        this.end = end;
        this.text = text;
    }

    public String getSpeaker() {
        return speaker;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public String getText() {
        return text;
    }
}
