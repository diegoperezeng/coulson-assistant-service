package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.Participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepositoryPort {

    Participant save(String name, String role, String voiceSampleKey);

    Optional<Participant> findById(UUID id);

    List<Participant> findAll();

    Participant update(UUID id, String name, String role, String voiceSampleKey);

    Participant updateVoiceData(UUID id, String voiceId, String presentationAudioKey);

    void delete(UUID id);
}
