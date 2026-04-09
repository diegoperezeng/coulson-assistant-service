package com.yellowleaf.coulson.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "video")
public class VideoProperties {

    private long maxChunkSizeMb = 200;
    private double maxAudioChunkSeconds = 400;
    private String tempDir = "/tmp/coulson-videos";

    public long getMaxChunkSizeMb() {
        return maxChunkSizeMb;
    }

    public void setMaxChunkSizeMb(long maxChunkSizeMb) {
        this.maxChunkSizeMb = maxChunkSizeMb;
    }

    public long getMaxChunkSizeBytes() {
        return maxChunkSizeMb * 1024L * 1024L;
    }

    public double getMaxAudioChunkSeconds() {
        return maxAudioChunkSeconds;
    }

    public void setMaxAudioChunkSeconds(double maxAudioChunkSeconds) {
        this.maxAudioChunkSeconds = maxAudioChunkSeconds;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }
}
