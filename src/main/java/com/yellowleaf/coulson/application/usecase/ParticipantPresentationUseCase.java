package com.yellowleaf.coulson.application.usecase;

import com.yellowleaf.coulson.domain.model.Participant;
import com.yellowleaf.coulson.domain.port.ParticipantRepositoryPort;
import com.yellowleaf.coulson.domain.port.StoragePort;
import com.yellowleaf.coulson.domain.port.TextToSpeechPort;
import com.yellowleaf.coulson.domain.port.VoiceCloningPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.UUID;

public class ParticipantPresentationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ParticipantPresentationUseCase.class);

    private final ParticipantRepositoryPort participantRepository;
    private final StoragePort storagePort;
    private final VoiceCloningPort voiceCloningPort;
    private final TextToSpeechPort textToSpeechPort;
    private final String voiceSamplesBucket;
    private final String presentationAudioBucket;

    public ParticipantPresentationUseCase(
            ParticipantRepositoryPort participantRepository,
            StoragePort storagePort,
            VoiceCloningPort voiceCloningPort,
            TextToSpeechPort textToSpeechPort,
            String voiceSamplesBucket,
            String presentationAudioBucket) {
        this.participantRepository = participantRepository;
        this.storagePort = storagePort;
        this.voiceCloningPort = voiceCloningPort;
        this.textToSpeechPort = textToSpeechPort;
        this.voiceSamplesBucket = voiceSamplesBucket;
        this.presentationAudioBucket = presentationAudioBucket;
    }

    public Participant generate(UUID participantId) {
        Participant participant = findById(participantId);

        if (participant.getVoiceSampleKey() == null) {
            throw new IllegalStateException("Participant has no voice sample: " + participantId);
        }

        log.info("Generating presentation voice [participantId={}] name={}", participantId, participant.getName());

        byte[] sampleBytes = storagePort.download(voiceSamplesBucket, participant.getVoiceSampleKey());
        String sampleFileName = participant.getVoiceSampleKey().contains(".")
                ? participant.getVoiceSampleKey().substring(participant.getVoiceSampleKey().lastIndexOf('/') + 1)
                : "voice_sample.mp3";

        String voiceId = voiceCloningPort.createVoiceClone(participant.getName(), sampleBytes, sampleFileName);

        String presentationText = "Eu sou " + participant.getName() + " e eu sou um participante.";
        byte[] audioBytes = textToSpeechPort.generateSpeech(voiceId, presentationText);

        String audioKey = "participants/" + participantId + "/presentation.mp3";

        if (participant.getPresentationAudioKey() != null) {
            storagePort.delete(presentationAudioBucket, participant.getPresentationAudioKey());
        }

        storagePort.upload(presentationAudioBucket, audioKey, audioBytes, "audio/mpeg");

        Participant updated = participantRepository.updateVoiceData(participantId, voiceId, audioKey);
        log.info("Presentation voice generated [participantId={}] voiceId={}", participantId, voiceId);
        return updated;
    }

    public byte[] download(UUID participantId) {
        Participant participant = findById(participantId);
        if (participant.getPresentationAudioKey() == null) {
            throw new NoSuchElementException("No presentation audio for participant: " + participantId);
        }
        log.info("Downloading presentation audio [participantId={}]", participantId);
        return storagePort.download(presentationAudioBucket, participant.getPresentationAudioKey());
    }

    public Participant delete(UUID participantId) {
        Participant participant = findById(participantId);
        if (participant.getPresentationAudioKey() != null) {
            storagePort.delete(presentationAudioBucket, participant.getPresentationAudioKey());
        }
        Participant updated = participantRepository.updateVoiceData(participantId, participant.getVoiceId(), null);
        log.info("Presentation audio deleted [participantId={}]", participantId);
        return updated;
    }

    private Participant findById(UUID id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Participant not found: " + id));
    }
}
