package com.yellowleaf.coulson.domain.model;

import java.nio.file.Path;

public class AudioChunk {

    private final int index;
    private final String videoId;
    private final Path filePath;
    private final double startOffsetSeconds;

    public AudioChunk(int index, String videoId, Path filePath, double startOffsetSeconds) {
        this.index = index;
        this.videoId = videoId;
        this.filePath = filePath;
        this.startOffsetSeconds = startOffsetSeconds;
    }

    public int getIndex() {
        return index;
    }

    public String getVideoId() {
        return videoId;
    }

    public Path getFilePath() {
        return filePath;
    }

    public double getStartOffsetSeconds() {
        return startOffsetSeconds;
    }
}
