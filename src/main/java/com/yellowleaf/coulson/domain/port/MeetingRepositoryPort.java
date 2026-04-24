package com.yellowleaf.coulson.domain.port;

import com.yellowleaf.coulson.domain.model.Meeting;

import java.util.List;
import java.util.UUID;

public interface MeetingRepositoryPort {

    void save(Meeting meeting);

    List<SegmentRecord> findSegmentsWithoutEmbedding(String meetingId);

    void updateSegmentEmbedding(UUID segmentId, float[] embedding);

    void markMeetingAsEmbedded(String meetingId);

    record SegmentRecord(UUID id, String text) {}
}
