package com.yellowleaf.coulson.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "elevenlabs")
public class ElevenLabsProperties {

    private String apiKey;
    private String baseUrl = "https://api.elevenlabs.io";
    private String modelId = "eleven_multilingual_v2";
    private double stability = 0.5;
    private double similarityBoost = 0.75;
    private double style = 0.0;
    private double speed = 1.0;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }

    public double getStability() { return stability; }
    public void setStability(double stability) { this.stability = stability; }

    public double getSimilarityBoost() { return similarityBoost; }
    public void setSimilarityBoost(double similarityBoost) { this.similarityBoost = similarityBoost; }

    public double getStyle() { return style; }
    public void setStyle(double style) { this.style = style; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
}
