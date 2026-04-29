package com.yellowleaf.coulson.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yellowleaf.coulson.domain.port.VideoTranscodePort;
import com.yellowleaf.coulson.infrastructure.config.FfmpegProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FfmpegVideoTranscodeAdapter implements VideoTranscodePort {

    private static final Logger log = LoggerFactory.getLogger(FfmpegVideoTranscodeAdapter.class);

    private final FfmpegProperties ffmpegProperties;
    private final ObjectMapper objectMapper;
    private final int processTimeoutSeconds;

    public FfmpegVideoTranscodeAdapter(FfmpegProperties ffmpegProperties,
                                       ObjectMapper objectMapper,
                                       int processTimeoutSeconds) {
        this.ffmpegProperties = ffmpegProperties;
        this.objectMapper = objectMapper;
        this.processTimeoutSeconds = processTimeoutSeconds;
    }

    @Override
    public TranscodeResult transcode(Path input, Path output, int maxHeight, int targetFps,
                                     int crf, String preset, String audioBitrate) {
        try {
            Files.createDirectories(output.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create output directory: " + output.getParent(), e);
        }

        List<String> command = new ArrayList<>(List.of(
                ffmpegProperties.getPath(),
                "-i", input.toString(),
                "-vf", "scale=-2:" + maxHeight + ":force_original_aspect_ratio=decrease",
                "-r", String.valueOf(targetFps),
                "-c:v", "libx264",
                "-preset", preset,
                "-crf", String.valueOf(crf),
                "-profile:v", "high",
                "-level", "4.1",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-b:a", audioBitrate,
                "-ac", "1",
                "-ar", "48000",
                "-movflags", "+faststart",
                "-y",
                output.toString()
        ));

        log.info("Transcoding video [input={}] to [output={}] maxHeight={} crf={} preset={}",
                input.getFileName(), output.getFileName(), maxHeight, crf, preset);

        runCommand(command, "video transcode");

        try {
            log.info("Transcode complete [output={}] size={}MB",
                    output.getFileName(), Files.size(output) / (1024 * 1024));
        } catch (IOException e) {
            log.warn("Could not read output file size: {}", e.getMessage());
        }

        return probe(output);
    }

    private TranscodeResult probe(Path file) {
        List<String> command = List.of(
                ffmpegProperties.getFfprobePath(),
                "-v", "error",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                file.toString()
        );

        String json = runCommandOutput(command, "ffprobe");

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode format = root.path("format");

            double durationSec = format.path("duration").asDouble(0.0);
            long sizeBytes = Files.size(file);

            int width = 0;
            int height = 0;
            double fps = 0.0;

            for (JsonNode stream : root.path("streams")) {
                if ("video".equals(stream.path("codec_type").asText())) {
                    width = stream.path("width").asInt(0);
                    height = stream.path("height").asInt(0);
                    fps = parseFps(stream.path("r_frame_rate").asText("0/1"));
                    break;
                }
            }

            return new TranscodeResult((long) (durationSec * 1000), width, height, fps, sizeBytes);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse ffprobe output for: " + file, e);
        }
    }

    private static double parseFps(String fpsStr) {
        if (fpsStr == null || fpsStr.isBlank()) return 0.0;
        String[] parts = fpsStr.split("/");
        if (parts.length == 1) return Double.parseDouble(parts[0]);
        double num = Double.parseDouble(parts[0]);
        double den = Double.parseDouble(parts[1]);
        return den == 0.0 ? 0.0 : num / den;
    }

    private void runCommand(List<String> command, String description) {
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

            boolean finished = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(description + " timed out after " + processTimeoutSeconds + "s");
            }

            int exitCode = process.exitValue();
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

    private String runCommandOutput(List<String> command, String description) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(description + " timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException(description + " failed with exit code " + exitCode);
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
