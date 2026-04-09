package com.yellowleaf.coulson.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties {

    private String baseUrl;
    private String extractSoundPath;
    private String transcribePath;
    private String token;
    private int connectTimeoutSeconds = 30;
    private int readTimeoutSeconds = 300;
    private int transcriptionParallelism = 4;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getExtractSoundPath() {
        return extractSoundPath;
    }

    public void setExtractSoundPath(String extractSoundPath) {
        this.extractSoundPath = extractSoundPath;
    }

    public String getTranscribePath() {
        return transcribePath;
    }

    public void setTranscribePath(String transcribePath) {
        this.transcribePath = transcribePath;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public String getExtractSoundUrl() {
        return baseUrl + extractSoundPath;
    }

    public String getTranscribeUrl() {
        return baseUrl + transcribePath;
    }

    public int getTranscriptionParallelism() {
        return transcriptionParallelism;
    }

    public void setTranscriptionParallelism(int transcriptionParallelism) {
        this.transcriptionParallelism = transcriptionParallelism;
    }
}
