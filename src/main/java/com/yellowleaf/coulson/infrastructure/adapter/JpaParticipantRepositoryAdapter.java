package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.Participant;
import com.yellowleaf.coulson.domain.port.ParticipantRepositoryPort;
import com.yellowleaf.coulson.infrastructure.persistence.entity.ParticipantEntity;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringParticipantJpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

public class JpaParticipantRepositoryAdapter implements ParticipantRepositoryPort {

    private final SpringParticipantJpaRepository repository;

    public JpaParticipantRepositoryAdapter(SpringParticipantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Participant save(String name, String role, String voiceSampleKey) {
        ParticipantEntity entity = new ParticipantEntity();
        entity.setName(name);
        entity.setRole(role != null ? role : "indefinido");
        entity.setVoiceSampleKey(voiceSampleKey);
        entity.setCreatedAt(OffsetDateTime.now());
        return toParticipant(repository.save(entity));
    }

    @Override
    public Optional<Participant> findById(UUID id) {
        return repository.findById(id).map(this::toParticipant);
    }

    @Override
    public List<Participant> findAll() {
        return repository.findAll().stream().map(this::toParticipant).toList();
    }

    @Override
    public Participant update(UUID id, String name, String role, String voiceSampleKey) {
        ParticipantEntity entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Participant not found: " + id));
        if (name != null) entity.setName(name);
        if (role != null) entity.setRole(role);
        if (voiceSampleKey != null) entity.setVoiceSampleKey(voiceSampleKey);
        return toParticipant(repository.save(entity));
    }

    @Override
    public Participant updateVoiceData(UUID id, String voiceId, String presentationAudioKey) {
        ParticipantEntity entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Participant not found: " + id));
        entity.setVoiceId(voiceId);
        entity.setPresentationAudioKey(presentationAudioKey);
        return toParticipant(repository.save(entity));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Participant not found: " + id);
        }
        repository.deleteById(id);
    }

    private Participant toParticipant(ParticipantEntity e) {
        return new Participant(e.getId(), e.getName(), e.getRole(), e.getVoiceSampleKey(),
                e.getVoiceId(), e.getPresentationAudioKey(), e.getCreatedAt());
    }
}
