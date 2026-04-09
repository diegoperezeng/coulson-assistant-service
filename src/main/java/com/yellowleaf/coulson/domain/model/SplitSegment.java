package com.yellowleaf.coulson.domain.model;

public class SplitSegment {

    private final int index;
    private final double startSeconds;
    private final double durationSeconds;

    public SplitSegment(int index, double startSeconds, double durationSeconds) {
        this.index = index;
        this.startSeconds = startSeconds;
        this.durationSeconds = durationSeconds;
    }

    public int getIndex() {
        return index;
    }

    public double getStartSeconds() {
        return startSeconds;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }
}
