package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.Meeting;
import com.yellowleaf.coulson.domain.model.TranscriptionSegment;
import com.yellowleaf.coulson.domain.port.MeetingRepositoryPort;
import com.yellowleaf.coulson.infrastructure.persistence.entity.MeetingEntity;
import com.yellowleaf.coulson.infrastructure.persistence.entity.TranscriptionSegmentEntity;
import com.yellowleaf.coulson.infrastructure.persistence.repository.SpringMeetingJpaRepository;
import com.yellowleaf.coulson.infrastructure.persistence.repository.VectorUpdateRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class JpaMeetingRepositoryAdapter implements MeetingRepositoryPort {

    private final SpringMeetingJpaRepository jpaRepository;
    private final VectorUpdateRepository vectorRepository;

    public JpaMeetingRepositoryAdapter(
            SpringMeetingJpaRepository jpaRepository,
            VectorUpdateRepository vectorRepository) {
        this.jpaRepository = jpaRepository;
        this.vectorRepository = vectorRepository;
    }

    @Override
    public void save(Meeting meeting) {
        MeetingEntity entity = toEntity(meeting);
        jpaRepository.save(entity);
    }

    @Override
    public List<SegmentRecord> findSegmentsWithoutEmbedding(String meetingId) {
        return vectorRepository.findSegmentsWithoutEmbedding(meetingId);
    }

    @Override
    public void updateSegmentEmbedding(UUID segmentId, float[] embedding) {
        vectorRepository.updateEmbedding(segmentId, embedding);
    }

    @Override
    public void markMeetingAsEmbedded(String meetingId) {
        jpaRepository.markAsEmbedded(UUID.fromString(meetingId), OffsetDateTime.now());
    }

    private MeetingEntity toEntity(Meeting meeting) {
        MeetingEntity entity = new MeetingEntity();
        entity.setId(UUID.fromString(meeting.getId()));
        entity.setTitle(meeting.getTitle());
        entity.setRecordedAt(OffsetDateTime.now());
        entity.setDurationSec(BigDecimal.valueOf(meeting.getDurationSeconds()));
        entity.setFullText(meeting.getFullText());

        List<TranscriptionSegmentEntity> segmentEntities = meeting.getSegments().stream()
                .map(s -> toSegmentEntity(s, entity))
                .toList();

        entity.setSegments(segmentEntities);
        return entity;
    }

    private TranscriptionSegmentEntity toSegmentEntity(TranscriptionSegment segment, MeetingEntity meeting) {
        TranscriptionSegmentEntity entity = new TranscriptionSegmentEntity();
        entity.setMeeting(meeting);
        entity.setSpeaker(segment.getSpeaker());
        entity.setStartSec(BigDecimal.valueOf(segment.getStart()));
        entity.setEndSec(BigDecimal.valueOf(segment.getEnd()));
        entity.setText(segment.getText());
        return entity;
    }
}
