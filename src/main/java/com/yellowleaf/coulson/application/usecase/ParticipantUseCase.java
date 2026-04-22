package com.yellowleaf.coulson.application.usecase;

import com.yellowleaf.coulson.domain.model.Participant;
import com.yellowleaf.coulson.domain.port.ParticipantRepositoryPort;
import com.yellowleaf.coulson.domain.port.StoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public class ParticipantUseCase {

    private static final Logger log = LoggerFactory.getLogger(ParticipantUseCase.class);

    private final ParticipantRepositoryPort participantRepositoryPort;
    private final StoragePort storagePort;
    private final String voiceSamplesBucket;

    public ParticipantUseCase(
            ParticipantRepositoryPort participantRepositoryPort,
            StoragePort storagePort,
            String voiceSamplesBucket) {
        this.participantRepositoryPort = participantRepositoryPort;
        this.storagePort = storagePort;
        this.voiceSamplesBucket = voiceSamplesBucket;
    }

    public Participant create(String name, String role, byte[] voiceSampleBytes, String contentType, String fileExtension) {
        String voiceSampleKey = uploadVoiceSampleIfPresent(null, voiceSampleBytes, contentType, fileExtension);
        String resolvedRole = (role != null && !role.isBlank()) ? role : "indefinido";
        return participantRepositoryPort.save(name, resolvedRole, voiceSampleKey);
    }

    public Participant findById(UUID id) {
        return participantRepositoryPort.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Participant not found: " + id));
    }

    public List<Participant> findAll() {
        return participantRepositoryPort.findAll();
    }

    public Participant update(UUID id, String name, String role, byte[] voiceSampleBytes, String contentType, String fileExtension) {
        Participant existing = findById(id);

        String voiceSampleKey = existing.getVoiceSampleKey();

        if (voiceSampleBytes != null) {
            if (voiceSampleKey != null) {
                storagePort.delete(voiceSamplesBucket, voiceSampleKey);
            }
            voiceSampleKey = uploadVoiceSampleIfPresent(id, voiceSampleBytes, contentType, fileExtension);
        }

        return participantRepositoryPort.update(id, name, role, voiceSampleKey);
    }

    public void delete(UUID id) {
        Participant participant = findById(id);
        if (participant.getVoiceSampleKey() != null) {
            storagePort.delete(voiceSamplesBucket, participant.getVoiceSampleKey());
        }
        participantRepositoryPort.delete(id);
    }

    private String uploadVoiceSampleIfPresent(UUID participantId, byte[] bytes, String contentType, String fileExtension) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String suffix = (fileExtension != null && !fileExtension.isBlank()) ? fileExtension : "bin";
        String objectPath = (participantId != null ? participantId : UUID.randomUUID()) + "." + suffix;
        return storagePort.upload(voiceSamplesBucket, objectPath, bytes, contentType);
    }
}
