package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.model.VideoChunk;
import com.yellowleaf.coulson.domain.port.AudioExtractorPort;
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
import java.util.List;

public class FfmpegAudioExtractorAdapter implements AudioExtractorPort {

    private static final Logger log = LoggerFactory.getLogger(FfmpegAudioExtractorAdapter.class);

    private final FfmpegProperties ffmpegProperties;
    private final VideoProperties videoProperties;

    public FfmpegAudioExtractorAdapter(FfmpegProperties ffmpegProperties, VideoProperties videoProperties) {
        this.ffmpegProperties = ffmpegProperties;
        this.videoProperties = videoProperties;
    }

    @Override
    public AudioChunk extract(VideoChunk videoChunk) {
        try {
            Path tempDir = Paths.get(videoProperties.getTempDir());
            Files.createDirectories(tempDir);

            String outputName = videoChunk.getVideoId() + "-audio-" + videoChunk.getIndex() + ".mp3";
            Path outputPath = tempDir.resolve(outputName);

            List<String> command = List.of(
                    ffmpegProperties.getPath(),
                    "-i", videoChunk.getFilePath().toString(),
                    "-vn",
                    "-ar", String.valueOf(ffmpegProperties.getAudioSampleRate()),
                    "-ac", String.valueOf(ffmpegProperties.getAudioChannels()),
                    "-b:a", ffmpegProperties.getAudioBitrate(),
                    "-y",
                    outputPath.toString()
            );

            executeCommand(command, "audio extraction chunk " + videoChunk.getIndex());

            long sizeKb = Files.size(outputPath) / 1024;
            log.info("Audio extracted [chunk={} videoId={}] size={}KB output={}",
                    videoChunk.getIndex(), videoChunk.getVideoId(), sizeKb, outputPath.getFileName());

            return new AudioChunk(videoChunk.getIndex(), videoChunk.getVideoId(), outputPath, 0.0);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to create temp dir for audio extraction chunk " + videoChunk.getIndex(), e);
        }
    }

    private void executeCommand(List<String> command, String description) {
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
                throw new IllegalStateException(description + " failed with exit code " + exitCode + ": " + output);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute: " + description, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing: " + description, e);
        }
    }
}
