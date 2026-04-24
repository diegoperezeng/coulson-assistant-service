package com.yellowleaf.coulson.domain.event;

public class MeetingTranscribedEvent {

    private final String meetingId;

    public MeetingTranscribedEvent(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getMeetingId() {
        return meetingId;
    }
}
