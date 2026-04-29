package com.yellowleaf.coulson.infrastructure.adapter.elevenlabs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ElevenLabsVoiceSettings {

    private double stability;

    @JsonProperty("similarity_boost")
    private double similarityBoost;

    private double style;
    private double speed;

    public ElevenLabsVoiceSettings(double stability, double similarityBoost, double style, double speed) {
        this.stability = stability;
        this.similarityBoost = similarityBoost;
        this.style = style;
        this.speed = speed;
    }

    public double getStability() { return stability; }
    public double getSimilarityBoost() { return similarityBoost; }
    public double getStyle() { return style; }
    public double getSpeed() { return speed; }
}
