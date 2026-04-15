package com.yellowleaf.coulson.domain.model;

public class SpeakerMapping {

    private final String speaker;
    private final String name;

    public SpeakerMapping(String speaker, String name) {
        this.speaker = speaker;
        this.name = name;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getName() {
        return name;
    }
}
