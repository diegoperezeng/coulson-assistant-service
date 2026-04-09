package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.SplitPlan;
import com.yellowleaf.coulson.domain.model.SplitSegment;
import com.yellowleaf.coulson.domain.model.Video;
import com.yellowleaf.coulson.domain.model.VideoChunk;
import com.yellowleaf.coulson.domain.port.VideoSplitterPort;
import com.yellowleaf.coulson.domain.service.VideoSplitPlanService;
import com.yellowleaf.coulson.infrastructure.config.FfmpegProperties;
import com.yellowleaf.coulson.infrastructure.config.VideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FfmpegVideoSplitterAdapter implements VideoSplitterPort {

    private static final Logger log = LoggerFactory.getLogger(FfmpegVideoSplitterAdapter.class);

    private final FfmpegProperties ffmpegProperties;
    private final VideoProperties videoProperties;
    private final VideoSplitPlanService splitPlanService;

    public FfmpegVideoSplitterAdapter(
            FfmpegProperties ffmpegProperties,
            VideoProperties videoProperties,
            VideoSplitPlanService splitPlanService) {
        this.ffmpegProperties = ffmpegProperties;
        this.videoProperties = videoProperties;
        this.splitPlanService = splitPlanService;
    }

    @Override
    public List<VideoChunk> split(Video video) {
        double duration = probeDuration(video.getFilePath());
        video.setDurationSeconds(duration);

        SplitPlan plan = splitPlanService.computePlan(video, videoProperties.getMaxChunkSizeBytes());

        if (!plan.requiresSplitting()) {
            log.info("Video [id={}] does not require splitting", video.getId());
            return List.of(new VideoChunk(0, video.getId(), video.getFilePath(), true));
        }

        log.info("Splitting video [id={}] into {} segments", video.getId(), plan.getSegments().size());
        return executeFFmpegSplit(video, plan);
    }

    private List<VideoChunk> executeFFmpegSplit(Video video, SplitPlan plan) {
        Path tempDir = Paths.get(videoProperties.getTempDir());
        String extension = video.getFileExtension();
        List<VideoChunk> chunks = new ArrayList<>();

        for (SplitSegment segment : plan.getSegments()) {
            String chunkName = video.getId() + "-chunk-" + segment.getIndex() + extension;
            Path chunkPath = tempDir.resolve(chunkName);

            List<String> command = List.of(
                    ffmpegProperties.getPath(),
                    "-i", video.getFilePath().toString(),
                    "-ss", String.valueOf(segment.getStartSeconds()),
                    "-t", String.valueOf(segment.getDurationSeconds()),
                    "-c", "copy",
                    "-y",
                    chunkPath.toString()
            );

            executeCommand(command, "ffmpeg split segment " + segment.getIndex());
            chunks.add(new VideoChunk(segment.getIndex(), video.getId(), chunkPath, false));
        }

        return chunks;
    }

    private double probeDuration(Path videoPath) {
        List<String> command = List.of(
                ffmpegProperties.getFfprobePath(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoPath.toString()
        );

        String output = executeCommand(command, "ffprobe duration").trim();

        try {
            return Double.parseDouble(output);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Could not parse video duration from ffprobe output: " + output);
        }
    }

    private String executeCommand(List<String> command, String description) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException(
                        description + " failed with exit code " + exitCode + ": " + output);
            }

            return output.toString();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute: " + description, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing: " + description, e);
        }
    }
}
