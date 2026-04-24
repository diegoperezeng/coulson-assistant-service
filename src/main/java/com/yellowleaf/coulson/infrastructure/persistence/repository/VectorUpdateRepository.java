package com.yellowleaf.coulson.infrastructure.persistence.repository;

import com.yellowleaf.coulson.domain.port.MeetingRepositoryPort.SegmentRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class VectorUpdateRepository {

    private final JdbcTemplate jdbcTemplate;

    public VectorUpdateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SegmentRecord> findSegmentsWithoutEmbedding(String meetingId) {
        return jdbcTemplate.query(
                "SELECT id, text FROM transcription_segments WHERE meeting_id = ?::uuid AND embedding IS NULL",
                (rs, rowNum) -> new SegmentRecord(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("text")
                ),
                meetingId
        );
    }

    public void updateEmbedding(UUID segmentId, float[] embedding) {
        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update(
                "UPDATE transcription_segments SET embedding = ?::vector WHERE id = ?",
                vectorLiteral,
                segmentId
        );
    }

    private String toVectorLiteral(float[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
