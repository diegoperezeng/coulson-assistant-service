package com.yellowleaf.coulson.infrastructure.adapter.elevenlabs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ElevenLabsTtsRequest {

    private String text;

    @JsonProperty("model_id")
    private String modelId;

    @JsonProperty("language_code")
    private String languageCode;

    @JsonProperty("voice_settings")
    private ElevenLabsVoiceSettings voiceSettings;

    public ElevenLabsTtsRequest(String text, String modelId, String languageCode, ElevenLabsVoiceSettings voiceSettings) {
        this.text = text;
        this.modelId = modelId;
        this.languageCode = languageCode;
        this.voiceSettings = voiceSettings;
    }

    public String getText() { return text; }
    public String getModelId() { return modelId; }
    public String getLanguageCode() { return languageCode; }
    public ElevenLabsVoiceSettings getVoiceSettings() { return voiceSettings; }
}
