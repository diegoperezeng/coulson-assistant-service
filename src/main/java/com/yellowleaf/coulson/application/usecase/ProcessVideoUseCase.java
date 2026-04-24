package com.yellowleaf.coulson.application.usecase;

import com.yellowleaf.coulson.application.command.ProcessVideoCommand;
import com.yellowleaf.coulson.application.result.TranscriptionResult;
import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.model.Transcription;
import com.yellowleaf.coulson.domain.model.TranscriptionSegment;
import com.yellowleaf.coulson.domain.model.Video;
import com.yellowleaf.coulson.domain.model.VideoChunk;
import com.yellowleaf.coulson.domain.event.MeetingTranscribedEvent;
import com.yellowleaf.coulson.domain.model.Meeting;
import com.yellowleaf.coulson.domain.model.SpeakerMapping;
import com.yellowleaf.coulson.domain.port.AudioExtractorPort;
import com.yellowleaf.coulson.domain.port.AudioMergerPort;
import com.yellowleaf.coulson.domain.port.AudioSplitterPort;
import com.yellowleaf.coulson.domain.port.MeetingRepositoryPort;
import com.yellowleaf.coulson.domain.port.SpeakerResolutionWebhookPort;
import com.yellowleaf.coulson.domain.port.TranscriptionWebhookPort;
import com.yellowleaf.coulson.domain.port.VideoSplitterPort;
import com.yellowleaf.coulson.domain.port.VideoStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

public class ProcessVideoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessVideoUseCase.class);

    private final VideoStoragePort storagePort;
    private final VideoSplitterPort splitterPort;
    private final AudioExtractorPort audioExtractorPort;
    private final AudioMergerPort audioMergerPort;
    private final AudioSplitterPort audioSplitterPort;
    private final TranscriptionWebhookPort transcriptionWebhookPort;
    private final SpeakerResolutionWebhookPort speakerResolutionWebhookPort;
    private final MeetingRepositoryPort meetingRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final ExecutorService executor;
    private final double maxAudioChunkSeconds;

    public ProcessVideoUseCase(
            VideoStoragePort storagePort,
            VideoSplitterPort splitterPort,
            AudioExtractorPort audioExtractorPort,
            AudioMergerPort audioMergerPort,
            AudioSplitterPort audioSplitterPort,
            TranscriptionWebhookPort transcriptionWebhookPort,
            SpeakerResolutionWebhookPort speakerResolutionWebhookPort,
            MeetingRepositoryPort meetingRepositoryPort,
            ApplicationEventPublisher eventPublisher,
            ExecutorService executor,
            double maxAudioChunkSeconds) {
        this.storagePort = storagePort;
        this.splitterPort = splitterPort;
        this.audioExtractorPort = audioExtractorPort;
        this.audioMergerPort = audioMergerPort;
        this.audioSplitterPort = audioSplitterPort;
        this.transcriptionWebhookPort = transcriptionWebhookPort;
        this.speakerResolutionWebhookPort = speakerResolutionWebhookPort;
        this.meetingRepositoryPort = meetingRepositoryPort;
        this.eventPublisher = eventPublisher;
        this.executor = executor;
        this.maxAudioChunkSeconds = maxAudioChunkSeconds;
    }

    public TranscriptionResult execute(ProcessVideoCommand command) {
        log.info("Starting video processing for file: {}", command.getFile().getOriginalFilename());

        Video video = storagePort.store(command.getFile());
        log.info("Video stored [id={}] size={}MB", video.getId(), video.getSizeInBytes() / (1024 * 1024));

        List<Path> filesToCleanup = new ArrayList<>();
        filesToCleanup.add(video.getFilePath());

        try {
            List<VideoChunk> videoChunks = splitterPort.split(video);
            log.info("Video [id={}] split into {} chunk(s)", video.getId(), videoChunks.size());

            if (videoChunks.size() > 1) {
                videoChunks.forEach(c -> filesToCleanup.add(c.getFilePath()));
            }

            log.info("Extracting audio from {} chunk(s) in parallel [videoId={}]",
                    videoChunks.size(), video.getId());

            List<CompletableFuture<AudioChunk>> extractionFutures = videoChunks.stream()
                    .map(videoChunk -> CompletableFuture.supplyAsync(
                            () -> {
                                log.info("Extracting audio chunk {} [videoId={}]",
                                        videoChunk.getIndex(), video.getId());
                                return audioExtractorPort.extract(videoChunk);
                            },
                            executor
                    ))
                    .toList();

            List<AudioChunk> extractedAudioChunks = joinAllOrCancel(
                    extractionFutures, "Audio extraction failed", video.getId())
                    .stream()
                    .sorted(Comparator.comparingInt(AudioChunk::getIndex))
                    .toList();

            extractedAudioChunks.forEach(c -> filesToCleanup.add(c.getFilePath()));
            log.info("All {} audio chunk(s) extracted [videoId={}]", extractedAudioChunks.size(), video.getId());

            AudioChunk mergedAudio = audioMergerPort.merge(video.getId(), extractedAudioChunks);
            if (mergedAudio.getIndex() == -1) {
                filesToCleanup.add(mergedAudio.getFilePath());
            }

            long audioSizeBytes;
            try {
                audioSizeBytes = Files.size(mergedAudio.getFilePath());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read audio file size", e);
            }
            long audioSizeMb = audioSizeBytes / (1024 * 1024);
            System.out.println("--------------------------------");
            System.out.println("Audio final: " + mergedAudio.getFilePath().getFileName());
            System.out.println("Tamanho: " + audioSizeMb + "MB (" + audioSizeBytes + " bytes)");
            System.out.println("--------------------------------");
            log.info("Final audio ready [videoId={}] size={}MB", video.getId(), audioSizeMb);

            if (audioSizeBytes > 25L * 1024 * 1024) {
                throw new IllegalStateException(
                        "Arquivo muito extenso para transcrição. Tamanho do áudio final: " + audioSizeMb + "MB");
            }

            List<AudioChunk> audioChunksForTranscription = audioSplitterPort.split(mergedAudio, maxAudioChunkSeconds);
            log.info("Audio split into {} transcription chunk(s), dispatching in parallel [videoId={}]",
                    audioChunksForTranscription.size(), video.getId());

            if (audioChunksForTranscription.size() > 1) {
                audioChunksForTranscription.forEach(c -> filesToCleanup.add(c.getFilePath()));
            }

            List<CompletableFuture<ChunkTranscription>> transcriptionFutures = audioChunksForTranscription.stream()
                    .map(chunk -> CompletableFuture.supplyAsync(
                            () -> {
                                log.info("Transcribing chunk {} offset={}s [videoId={}]",
                                        chunk.getIndex(), chunk.getStartOffsetSeconds(), video.getId());
                                Transcription transcription = transcriptionWebhookPort.transcribe(chunk);
                                log.info("Chunk {} transcribed segments={} [videoId={}]",
                                        chunk.getIndex(), transcription.getSegments().size(), video.getId());

                                List<TranscriptionSegment> resolvedSegments = resolveSegmentSpeakers(
                                        transcription.getSegments(), video.getId(), chunk.getIndex());

                                return new ChunkTranscription(chunk, transcription.getText(), resolvedSegments);
                            },
                            executor
                    ))
                    .toList();

            List<ChunkTranscription> transcriptionResults = joinAllOrCancel(
                    transcriptionFutures, "Transcription failed for one or more chunks", video.getId())
                    .stream()
                    .sorted(Comparator.comparingDouble(ct -> ct.chunk().getStartOffsetSeconds()))
                    .toList();

            List<TranscriptionSegment> allSegments = new ArrayList<>();
            StringBuilder combinedText = new StringBuilder();

            for (ChunkTranscription ct : transcriptionResults) {
                double offset = ct.chunk().getStartOffsetSeconds();

                if (!combinedText.isEmpty()) {
                    combinedText.append(" ");
                }
                combinedText.append(ct.text());

                for (TranscriptionSegment segment : ct.resolvedSegments()) {
                    allSegments.add(new TranscriptionSegment(
                            segment.getSpeaker(),
                            segment.getStart() + offset,
                            segment.getEnd() + offset,
                            segment.getText()
                    ));
                }
            }

            log.info("Transcription complete [videoId={}] videoChunks={} audioChunks={} segments={}",
                    video.getId(), videoChunks.size(), audioChunksForTranscription.size(), allSegments.size());

            persistMeeting(video, combinedText.toString().trim(), allSegments);

            return new TranscriptionResult(
                    video.getId(), combinedText.toString().trim(), videoChunks.size(), allSegments);

        } finally {
            filesToCleanup.forEach(this::deleteQuietly);
        }
    }

    private void persistMeeting(Video video, String fullText, List<TranscriptionSegment> segments) {
        try {
            String title = video.getOriginalName();
            Meeting meeting = new Meeting(
                    video.getId(),
                    title,
                    video.getDurationSeconds(),
                    fullText,
                    segments
            );
            meetingRepositoryPort.save(meeting);
            eventPublisher.publishEvent(new MeetingTranscribedEvent(video.getId()));
            log.info("Meeting persisted [meetingId={}]", video.getId());
        } catch (Exception e) {
            log.error("Failed to persist meeting [meetingId={}]: {}", video.getId(), e.getMessage());
        }
    }

    private <T> List<T> joinAllOrCancel(List<CompletableFuture<T>> futures, String errorMessage, String videoId) {
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            all.join();
        } catch (CompletionException ex) {
            log.warn("Task failed, cancelling {} remaining future(s) [videoId={}]", futures.size(), videoId);
            futures.forEach(f -> f.cancel(true));
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IllegalStateException(errorMessage + ": " + cause.getMessage(), cause);
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", path);
        }
    }

    private List<TranscriptionSegment> resolveSegmentSpeakers(
            List<TranscriptionSegment> segments, String videoId, int chunkIndex) {

        if (segments.isEmpty()) {
            return segments;
        }

        log.info("Resolving speaker names for chunk {} [videoId={}]", chunkIndex, videoId);
        List<SpeakerMapping> mappings = speakerResolutionWebhookPort.resolve(segments);

        if (mappings.isEmpty()) {
            log.warn("No speaker mappings returned for chunk {} [videoId={}], keeping original codes",
                    chunkIndex, videoId);
            return segments;
        }

        return applyMappings(segments, mappings);
    }

    private List<TranscriptionSegment> applyMappings(
            List<TranscriptionSegment> segments, List<SpeakerMapping> mappings) {

        Map<String, String> speakerToName = mappings.stream()
                .collect(java.util.stream.Collectors.toMap(
                        SpeakerMapping::getSpeaker,
                        SpeakerMapping::getName,
                        (existing, replacement) -> existing
                ));

        return segments.stream()
                .map(s -> new TranscriptionSegment(
                        speakerToName.getOrDefault(s.getSpeaker(), s.getSpeaker()),
                        s.getStart(),
                        s.getEnd(),
                        s.getText()
                ))
                .toList();
    }

    private record ChunkTranscription(AudioChunk chunk, String text, List<TranscriptionSegment> resolvedSegments) {}
}
