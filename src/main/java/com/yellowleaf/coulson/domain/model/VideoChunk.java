package com.yellowleaf.coulson.domain.model;

import java.nio.file.Path;

public class VideoChunk {

    private final int index;
    private final String videoId;
    private final Path filePath;
    private final boolean isOriginal;

    public VideoChunk(int index, String videoId, Path filePath, boolean isOriginal) {
        this.index = index;
        this.videoId = videoId;
        this.filePath = filePath;
        this.isOriginal = isOriginal;
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

    public boolean isOriginal() {
        return isOriginal;
    }
}
