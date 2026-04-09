package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.port.AudioMergerPort;
import com.yellowleaf.coulson.infrastructure.config.FfmpegProperties;
import com.yellowleaf.coulson.infrastructure.config.VideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

public class FfmpegAudioMergerAdapter implements AudioMergerPort {

    private static final Logger log = LoggerFactory.getLogger(FfmpegAudioMergerAdapter.class);

    private final FfmpegProperties ffmpegProperties;
    private final VideoProperties videoProperties;

    public FfmpegAudioMergerAdapter(FfmpegProperties ffmpegProperties, VideoProperties videoProperties) {
        this.ffmpegProperties = ffmpegProperties;
        this.videoProperties = videoProperties;
    }

    @Override
    public AudioChunk merge(String videoId, List<AudioChunk> chunks) {
        List<AudioChunk> sorted = chunks.stream()
                .sorted(Comparator.comparingInt(AudioChunk::getIndex))
                .toList();

        Path outputPath = Paths.get(videoProperties.getTempDir()).resolve(videoId + "-merged.mp3");

        if (sorted.size() == 1) {
            log.info("Single audio chunk, applying compression only [videoId={}]", videoId);
            compressSingleChunk(sorted.get(0).getFilePath(), outputPath);
        } else {
            log.info("Merging and compressing {} audio chunks [videoId={}]", sorted.size(), videoId);
            mergeAndCompress(videoId, sorted, outputPath);
        }

        long outputSizeKb = fileSizeKb(outputPath);
        log.info("Audio ready [videoId={}] size={}KB bitrate={} sampleRate={}Hz channels={}",
                videoId, outputSizeKb,
                ffmpegProperties.getAudioBitrate(),
                ffmpegProperties.getAudioSampleRate(),
                ffmpegProperties.getAudioChannels());

        return new AudioChunk(-1, videoId, outputPath, 0.0);
    }

    private void compressSingleChunk(Path input, Path output) {
        List<String> command = List.of(
                ffmpegProperties.getPath(),
                "-i", input.toString(),
                "-ar", String.valueOf(ffmpegProperties.getAudioSampleRate()),
                "-ac", String.valueOf(ffmpegProperties.getAudioChannels()),
                "-b:a", ffmpegProperties.getAudioBitrate(),
                "-y",
                output.toString()
        );
        executeCommand(command);
    }

    private void mergeAndCompress(String videoId, List<AudioChunk> sorted, Path output) {
        Path concatListPath = writeConcatList(videoId, sorted);
        try {
            List<String> command = List.of(
                    ffmpegProperties.getPath(),
                    "-f", "concat",
                    "-safe", "0",
                    "-i", concatListPath.toString(),
                    "-ar", String.valueOf(ffmpegProperties.getAudioSampleRate()),
                    "-ac", String.valueOf(ffmpegProperties.getAudioChannels()),
                    "-b:a", ffmpegProperties.getAudioBitrate(),
                    "-y",
                    output.toString()
            );
            executeCommand(command);
        } finally {
            deleteQuietly(concatListPath);
        }
    }

    private Path writeConcatList(String videoId, List<AudioChunk> sorted) {
        try {
            Path listPath = Paths.get(videoProperties.getTempDir()).resolve(videoId + "-concat.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(listPath.toFile()))) {
                for (AudioChunk chunk : sorted) {
                    writer.write("file '" + chunk.getFilePath().toAbsolutePath() + "'");
                    writer.newLine();
                }
            }
            return listPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write FFmpeg concat list", e);
        }
    }

    private void executeCommand(List<String> command) {
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
                throw new IllegalStateException("FFmpeg audio processing failed with exit code " + exitCode + ": " + output);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute FFmpeg audio processing", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing FFmpeg audio processing", e);
        }
    }

    private long fileSizeKb(Path path) {
        try {
            return Files.size(path) / 1024;
        } catch (IOException e) {
            return -1;
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", path);
        }
    }
}
