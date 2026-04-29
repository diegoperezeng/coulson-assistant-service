package com.yellowleaf.coulson.domain.port;

import java.nio.file.Path;

public interface VideoTranscodePort {

    TranscodeResult transcode(Path input, Path output, int maxHeight, int targetFps,
                              int crf, String preset, String audioBitrate);

    record TranscodeResult(long durationMs, int width, int height, double fps, long sizeBytes) {}
}
