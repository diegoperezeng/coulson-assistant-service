package com.yellowleaf.coulson.application.usecase;

import com.yellowleaf.coulson.domain.model.Participant;
import com.yellowleaf.coulson.domain.model.Team;
import com.yellowleaf.coulson.domain.port.StoragePort;
import com.yellowleaf.coulson.domain.port.TeamRepositoryPort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TeamPresentationUseCase {

    private static final Logger log = LoggerFactory.getLogger(TeamPresentationUseCase.class);

    private final TeamRepositoryPort teamRepository;
    private final StoragePort storagePort;
    private final FfmpegProperties ffmpegProperties;
    private final VideoProperties videoProperties;
    private final String presentationAudioBucket;

    public TeamPresentationUseCase(
            TeamRepositoryPort teamRepository,
            StoragePort storagePort,
            FfmpegProperties ffmpegProperties,
            VideoProperties videoProperties,
            String presentationAudioBucket) {
        this.teamRepository = teamRepository;
        this.storagePort = storagePort;
        this.ffmpegProperties = ffmpegProperties;
        this.videoProperties = videoProperties;
        this.presentationAudioBucket = presentationAudioBucket;
    }

    public Team generate(UUID teamId) {
        Team team = findById(teamId);

        if (team.getMembers().isEmpty()) {
            throw new IllegalStateException("Team has no members: " + teamId);
        }

        List<Participant> membersWithAudio = team.getMembers().stream()
                .map(m -> m.getParticipant())
                .filter(p -> p.getPresentationAudioKey() != null)
                .toList();

        if (membersWithAudio.isEmpty()) {
            throw new IllegalStateException("No team member has a presentation audio yet: " + teamId);
        }

        log.info("Generating team presentation audio [teamId={}] members with audio={}/{}",
                teamId, membersWithAudio.size(), team.getMembers().size());

        List<Path> tempFiles = new ArrayList<>();
        try {
            List<Path> memberAudios = downloadMemberAudios(membersWithAudio, teamId, tempFiles);
            Path concatenated = concatenateAudios(memberAudios, teamId);
            tempFiles.add(concatenated);

            double durationSeconds = getAudioDuration(concatenated);
            byte[] audioBytes = Files.readAllBytes(concatenated);

            String audioKey = "teams/" + teamId + "/presentation.mp3";
            if (team.getPresentationAudioKey() != null) {
                storagePort.delete(presentationAudioBucket, team.getPresentationAudioKey());
            }
            storagePort.upload(presentationAudioBucket, audioKey, audioBytes, "audio/mpeg");

            Team updated = teamRepository.updatePresentationAudio(teamId, audioKey, durationSeconds);
            log.info("Team presentation audio generated [teamId={}] duration={}s", teamId, durationSeconds);
            return updated;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate team presentation audio", e);
        } finally {
            tempFiles.forEach(this::deleteQuietly);
        }
    }

    public byte[] download(UUID teamId) {
        Team team = findById(teamId);
        if (team.getPresentationAudioKey() == null) {
            throw new NoSuchElementException("No presentation audio for team: " + teamId);
        }
        log.info("Downloading team presentation audio [teamId={}]", teamId);
        return storagePort.download(presentationAudioBucket, team.getPresentationAudioKey());
    }

    public Team delete(UUID teamId) {
        Team team = findById(teamId);
        if (team.getPresentationAudioKey() != null) {
            storagePort.delete(presentationAudioBucket, team.getPresentationAudioKey());
        }
        Team updated = teamRepository.updatePresentationAudio(teamId, null, 0.0);
        log.info("Team presentation audio deleted [teamId={}]", teamId);
        return updated;
    }

    private List<Path> downloadMemberAudios(List<Participant> members, UUID teamId, List<Path> tempFiles) throws IOException {
        List<Path> paths = new ArrayList<>();
        Path tempDir = Paths.get(videoProperties.getTempDir());
        Files.createDirectories(tempDir);

        for (Participant member : members) {
            byte[] audioBytes = storagePort.download(presentationAudioBucket, member.getPresentationAudioKey());
            Path tempFile = tempDir.resolve("team-" + teamId + "-member-" + member.getId() + ".mp3");
            Files.write(tempFile, audioBytes);
            tempFiles.add(tempFile);
            paths.add(tempFile);
            log.info("Downloaded presentation audio for member [participantId={}]", member.getId());
        }
        return paths;
    }

    private Path concatenateAudios(List<Path> audioPaths, UUID teamId) throws IOException {
        Path tempDir = Paths.get(videoProperties.getTempDir());
        Path outputPath = tempDir.resolve("team-" + teamId + "-presentation.mp3");

        if (audioPaths.size() == 1) {
            Files.copy(audioPaths.get(0), outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return outputPath;
        }

        Path concatList = tempDir.resolve("team-" + teamId + "-concat.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(concatList.toFile()))) {
            for (Path path : audioPaths) {
                writer.write("file '" + path.toAbsolutePath() + "'");
                writer.newLine();
            }
        }

        try {
            List<String> command = List.of(
                    ffmpegProperties.getPath(),
                    "-f", "concat",
                    "-safe", "0",
                    "-i", concatList.toString(),
                    "-c", "copy",
                    "-y",
                    outputPath.toString()
            );
            executeCommand(command, "team audio concatenation [teamId=" + teamId + "]");
            log.info("Team audio concatenated [teamId={}] files={}", teamId, audioPaths.size());
            return outputPath;
        } finally {
            deleteQuietly(concatList);
        }
    }

    private double getAudioDuration(Path audioPath) {
        try {
            List<String> command = List.of(
                    ffmpegProperties.getFfprobePath(),
                    "-v", "quiet",
                    "-show_entries", "format=duration",
                    "-of", "csv=p=0",
                    audioPath.toAbsolutePath().toString()
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.readLine();
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("ffprobe timed out getting duration for: " + audioPath);
            }

            if (output == null || output.isBlank()) {
                throw new IllegalStateException("ffprobe returned empty duration for: " + audioPath);
            }

            return Double.parseDouble(output.trim());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to get audio duration", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while getting audio duration", e);
        }
    }

    private void executeCommand(List<String> command, String context) {
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

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("FFmpeg timed out: " + context);
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("FFmpeg failed [" + context + "]: " + output);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to execute FFmpeg: " + context, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted: " + context, e);
        }
    }

    private Team findById(UUID id) {
        return teamRepository.findTeamById(id)
                .orElseThrow(() -> new NoSuchElementException("Team not found: " + id));
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", path);
        }
    }
}
