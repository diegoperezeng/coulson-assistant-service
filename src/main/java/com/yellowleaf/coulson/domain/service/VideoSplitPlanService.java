package com.yellowleaf.coulson.domain.service;

import com.yellowleaf.coulson.domain.model.SplitPlan;
import com.yellowleaf.coulson.domain.model.SplitSegment;
import com.yellowleaf.coulson.domain.model.Video;

import java.util.ArrayList;
import java.util.List;

public class VideoSplitPlanService {

    public SplitPlan computePlan(Video video, long maxChunkSizeBytes) {
        if (video.getSizeInBytes() <= maxChunkSizeBytes) {
            return SplitPlan.noSplit();
        }

        double durationSeconds = video.getDurationSeconds();
        double bytesPerSecond = (double) video.getSizeInBytes() / durationSeconds;
        double maxSecondsPerChunk = maxChunkSizeBytes / bytesPerSecond;
        int numChunks = (int) Math.ceil(durationSeconds / maxSecondsPerChunk);

        List<SplitSegment> segments = new ArrayList<>();
        for (int i = 0; i < numChunks; i++) {
            double start = i * maxSecondsPerChunk;
            double chunkDuration = Math.min(maxSecondsPerChunk, durationSeconds - start);
            segments.add(new SplitSegment(i, start, chunkDuration));
        }

        return SplitPlan.withSegments(segments);
    }
}
