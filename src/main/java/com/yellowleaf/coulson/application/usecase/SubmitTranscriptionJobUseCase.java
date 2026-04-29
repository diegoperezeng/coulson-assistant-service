package com.yellowleaf.coulson.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yellowleaf.coulson.application.result.TranscriptionResult;
import com.yellowleaf.coulson.application.usecase.ProcessVideoUseCase.TeamPresentationContext;
import com.yellowleaf.coulson.domain.model.Team;
import com.yellowleaf.coulson.domain.model.TranscriptionJob;
import com.yellowleaf.coulson.domain.model.Video;
import com.yellowleaf.coulson.domain.port.StoragePort;
import com.yellowleaf.coulson.domain.port.TeamRepositoryPort;
import com.yellowleaf.coulson.domain.port.TranscriptionJobRepositoryPort;
import com.yellowleaf.coulson.domain.port.VideoStoragePort;
import com.yellowleaf.coulson.infrastructure.config.VideoPersistProperties;
import com.yellowleaf.coulson.infrastructure.config.VideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SubmitTranscriptionJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmitTranscriptionJobUseCase.class);

    private final VideoStoragePort storagePort;
    private final TranscriptionJobRepositoryPort jobRepository;
    private final ProcessVideoUseCase processVideoUseCase;
    private final PersistVideoUseCase persistVideoUseCase;
    private final VideoPersistProperties videoPersistProperties;
    private final ObjectMapper objectMapper;
    private final Executor jobExecutor;
    private final TeamRepositoryPort teamRepository;
    private final StoragePort presentationStoragePort;
    private final String presentationAudioBucket;
    private final String tempDir;

    public SubmitTranscriptionJobUseCase(
            VideoStoragePort storagePort,
            TranscriptionJobRepositoryPort jobRepository,
            ProcessVideoUseCase processVideoUseCase,
            PersistVideoUseCase persistVideoUseCase,
            VideoPersistProperties videoPersistProperties,
            ObjectMapper objectMapper,
            Executor jobExecutor,
            TeamRepositoryPort teamRepository,
            StoragePort presentationStoragePort,
            String presentationAudioBucket,
            String tempDir) {
        this.storagePort = storagePort;
        this.jobRepository = jobRepository;
        this.processVideoUseCase = processVideoUseCase;
        this.persistVideoUseCase = persistVideoUseCase;
        this.videoPersistProperties = videoPersistProperties;
        this.objectMapper = objectMapper;
        this.jobExecutor = jobExecutor;
        this.teamRepository = teamRepository;
        this.presentationStoragePort = presentationStoragePort;
        this.presentationAudioBucket = presentationAudioBucket;
        this.tempDir = tempDir;
    }

    public TranscriptionJob submit(MultipartFile file, UUID teamId, boolean force) {
        String fileHash = computeHash(file);
        log.info("File hash computed [hash={}] force={}", fileHash, force);

        if (!force) {
            Optional<TranscriptionJob> existing = jobRepository.findDoneByFileHash(fileHash);
            if (existing.isPresent()) {
                log.info("Duplicate file detected, returning existing job [jobId={}] hash={}",
                        existing.get().getId(), fileHash);
                return existing.get();
            }
        }

        Video video = storagePort.store(file);
        log.info("Video stored for async processing [videoId={}] file={}", video.getId(), video.getOriginalName());

        Path persistCopy = makePersistCopyIfEnabled(video, teamId);

        TranscriptionJob job = jobRepository.create(video.getOriginalName(), fileHash);
        log.info("Transcription job created [jobId={}] hash={}", job.getId(), fileHash);

        CompletableFuture.runAsync(() -> processAsync(job.getId(), video, teamId, persistCopy), jobExecutor);

        return job;
    }

    private String computeHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = new DigestInputStream(file.getInputStream(), digest)) {
                byte[] buffer = new byte[8192];
                while (is.read(buffer) != -1) { }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compute file hash", e);
        }
    }

    private void processAsync(UUID jobId, Video video, UUID teamId, Path persistCopy) {
        log.info("Async transcription started [jobId={}] videoId={}", jobId, video.getId());
        boolean transcriptionSucceeded = false;
        Path presentationTempPath = null;
        try {
            TeamPresentationContext teamPresentation = null;
            if (teamId != null) {
                teamPresentation = resolveTeamPresentation(teamId, video.getId());
                if (teamPresentation != null) {
                    presentationTempPath = teamPresentation.audioPath();
                }
            }

            TranscriptionResult result = processVideoUseCase.process(video, teamPresentation);
            String resultJson = serialize(result);
            jobRepository.markDone(jobId, resultJson);
            transcriptionSucceeded = true;
            log.info("Transcription done [jobId={}] videoId={} chunks={} segments={}",
                    jobId, result.getVideoId(), result.getTotalChunks(), result.getSegments().size());
            log.debug("Transcription text [jobId={}]: {}", jobId, result.getTranscription());
            log.debug("Transcription segments [jobId={}]: {}", jobId, result.getSegments());
        } catch (Exception e) {
            log.error("Async transcription failed [jobId={}]: {}", jobId, e.getMessage());
            jobRepository.markFailed(jobId, e.getMessage());
        } finally {
            deleteQuietly(presentationTempPath);
        }

        if (persistCopy != null) {
            if (transcriptionSucceeded && teamId != null) {
                persistVideoUseCase.persistAsync(jobId.toString(), teamId, persistCopy);
            } else {
                deleteQuietly(persistCopy);
            }
        }
    }

    private TeamPresentationContext resolveTeamPresentation(UUID teamId, String videoId) {
        try {
            Optional<Team> teamOpt = teamRepository.findTeamById(teamId);
            if (teamOpt.isEmpty()) {
                log.warn("Team not found for presentation audio [teamId={}] [videoId={}]", teamId, videoId);
                return null;
            }
            Team team = teamOpt.get();
            if (team.getPresentationAudioKey() == null) {
                log.info("Team has no presentation audio [teamId={}] [videoId={}]", teamId, videoId);
                return null;
            }

            log.info("Downloading team presentation audio [teamId={}] [videoId={}]", teamId, videoId);
            byte[] audioBytes = presentationStoragePort.download(
                    presentationAudioBucket, team.getPresentationAudioKey());

            Path tempFile = Paths.get(tempDir).resolve("team-pres-" + teamId + "-" + videoId + ".mp3");
            Files.createDirectories(tempFile.getParent());
            Files.write(tempFile, audioBytes);

            double duration = team.getPresentationAudioDurationSeconds() != null
                    ? team.getPresentationAudioDurationSeconds()
                    : 0.0;
            log.info("Team presentation audio ready [teamId={}] duration={}s [videoId={}]", teamId, duration, videoId);
            return new TeamPresentationContext(tempFile, duration);
        } catch (Exception e) {
            log.warn("Failed to resolve team presentation audio [teamId={}] [videoId={}]: {}",
                    teamId, videoId, e.getMessage());
            return null;
        }
    }

    private Path makePersistCopyIfEnabled(Video video, UUID teamId) {
        if (!videoPersistProperties.isEnabled() || teamId == null) return null;
        try {
            Path source = video.getFilePath();
            Path copy = source.getParent().resolve(video.getId() + "-persist" + video.getFileExtension());
            Files.copy(source, copy, StandardCopyOption.REPLACE_EXISTING);
            log.info("Persist copy created [videoId={}] path={}", video.getId(), copy.getFileName());
            return copy;
        } catch (IOException e) {
            log.warn("Failed to create persist copy for videoId={}, video persistence will be skipped: {}",
                    video.getId(), e.getMessage());
            return null;
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete file [{}]: {}", path, e.getMessage());
        }
    }

    private String serialize(TranscriptionResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transcription result", e);
        }
    }
}
