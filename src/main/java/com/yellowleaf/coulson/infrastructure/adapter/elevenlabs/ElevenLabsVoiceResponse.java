package com.yellowleaf.coulson.infrastructure.adapter.elevenlabs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ElevenLabsVoiceResponse {

    @JsonProperty("voice_id")
    private String voiceId;

    public String getVoiceId() { return voiceId; }
    public void setVoiceId(String voiceId) { this.voiceId = voiceId; }
}
