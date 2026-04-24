package com.yellowleaf.coulson.application.event;

import com.yellowleaf.coulson.domain.event.MeetingTranscribedEvent;
import com.yellowleaf.coulson.domain.port.EmbeddingPort;
import com.yellowleaf.coulson.domain.port.MeetingRepositoryPort;
import com.yellowleaf.coulson.domain.port.MeetingRepositoryPort.SegmentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnBean(EmbeddingPort.class)
public class EmbeddingEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingEventListener.class);

    private final EmbeddingPort embeddingPort;
    private final MeetingRepositoryPort meetingRepositoryPort;

    public EmbeddingEventListener(EmbeddingPort embeddingPort, MeetingRepositoryPort meetingRepositoryPort) {
        this.embeddingPort = embeddingPort;
        this.meetingRepositoryPort = meetingRepositoryPort;
    }

    @Async("embeddingExecutor")
    @EventListener
    public void onMeetingTranscribed(MeetingTranscribedEvent event) {
        String meetingId = event.getMeetingId();
        log.info("Starting embedding generation [meetingId={}]", meetingId);

        List<SegmentRecord> segments = meetingRepositoryPort.findSegmentsWithoutEmbedding(meetingId);
        if (segments.isEmpty()) {
            log.warn("No segments found for embedding [meetingId={}]", meetingId);
            return;
        }

        int success = 0;
        for (SegmentRecord segment : segments) {
            try {
                float[] embedding = embeddingPort.embed(segment.text());
                meetingRepositoryPort.updateSegmentEmbedding(segment.id(), embedding);
                success++;
            } catch (Exception e) {
                log.error("Failed to embed segment [segmentId={} meetingId={}]: {}",
                        segment.id(), meetingId, e.getMessage());
            }
        }

        if (success == segments.size()) {
            meetingRepositoryPort.markMeetingAsEmbedded(meetingId);
            log.info("Embedding complete [meetingId={}] segments={}", meetingId, success);
        } else {
            log.warn("Partial embedding [meetingId={}] success={} total={}", meetingId, success, segments.size());
        }
    }
}
