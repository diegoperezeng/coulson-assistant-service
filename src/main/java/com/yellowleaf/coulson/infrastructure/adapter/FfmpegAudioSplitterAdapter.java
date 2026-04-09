package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.port.AudioSplitterPort;
import com.yellowleaf.coulson.infrastructure.config.FfmpegProperties;
import com.yellowleaf.coulson.infrastructure.config.VideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FfmpegAudioSplitterAdapter implements AudioSplitterPort {

    private static final Logger log = LoggerFactory.getLogger(FfmpegAudioSplitterAdapter.class);

    private final FfmpegProperties ffmpegProperties;
    private final VideoProperties videoProperties;

    public FfmpegAudioSplitterAdapter(FfmpegProperties ffmpegProperties, VideoProperties videoProperties) {
        this.ffmpegProperties = ffmpegProperties;
        this.videoProperties = videoProperties;
    }

    @Override
    public List<AudioChunk> split(AudioChunk audio, double maxDurationSeconds) {
        double totalDuration = probeDuration(audio.getFilePath().toString());
        log.info("Audio duration: {}s max chunk: {}s [videoId={}]", totalDuration, maxDurationSeconds, audio.getVideoId());

        if (totalDuration <= maxDurationSeconds) {
            log.info("Audio does not require splitting [videoId={}]", audio.getVideoId());
            return List.of(audio);
        }

        int numChunks = (int) Math.ceil(totalDuration / maxDurationSeconds);
        log.info("Splitting audio into {} chunk(s) [videoId={}]", numChunks, audio.getVideoId());

        List<AudioChunk> chunks = new ArrayList<>();
        for (int i = 0; i < numChunks; i++) {
            double startSeconds = i * maxDurationSeconds;
            double chunkDuration = Math.min(maxDurationSeconds, totalDuration - startSeconds);

            String chunkName = audio.getVideoId() + "-audio-chunk-" + i + ".mp3";
            String chunkPath = Paths.get(videoProperties.getTempDir()).resolve(chunkName).toString();

            List<String> command = List.of(
                    ffmpegProperties.getPath(),
                    "-i", audio.getFilePath().toString(),
                    "-ss", String.valueOf(startSeconds),
                    "-t", String.valueOf(chunkDuration),
                    "-c", "copy",
                    "-y",
                    chunkPath
            );

            executeCommand(command, "audio split chunk " + i);
            log.info("Audio chunk {} created offset={}s duration={}s [videoId={}]",
                    i, startSeconds, chunkDuration, audio.getVideoId());

            chunks.add(new AudioChunk(i, audio.getVideoId(), Paths.get(chunkPath), startSeconds));
        }

        return chunks;
    }

    private double probeDuration(String filePath) {
        List<String> command = List.of(
                ffmpegProperties.getFfprobePath(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
        );

        String output = executeCommand(command, "ffprobe audio duration").trim();
        try {
            return Double.parseDouble(output);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Could not parse audio duration from ffprobe output: " + output);
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
                throw new IllegalStateException(description + " failed with exit code " + exitCode + ": " + output);
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
