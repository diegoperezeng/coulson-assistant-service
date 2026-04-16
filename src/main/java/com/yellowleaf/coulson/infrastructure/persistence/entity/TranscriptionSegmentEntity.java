package com.yellowleaf.coulson.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transcription_segments")
public class TranscriptionSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingEntity meeting;

    @Column(nullable = false, length = 255)
    private String speaker;

    @Column(name = "start_sec", nullable = false, precision = 10, scale = 3)
    private BigDecimal startSec;

    @Column(name = "end_sec", nullable = false, precision = 10, scale = 3)
    private BigDecimal endSec;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MeetingEntity getMeeting() { return meeting; }
    public void setMeeting(MeetingEntity meeting) { this.meeting = meeting; }

    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }

    public BigDecimal getStartSec() { return startSec; }
    public void setStartSec(BigDecimal startSec) { this.startSec = startSec; }

    public BigDecimal getEndSec() { return endSec; }
    public void setEndSec(BigDecimal endSec) { this.endSec = endSec; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
