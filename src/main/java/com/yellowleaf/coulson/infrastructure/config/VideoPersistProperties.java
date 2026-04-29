package com.yellowleaf.coulson.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "video.persist")
public class VideoPersistProperties {

    private boolean enabled = false;
    private int maxHeight = 540;
    private int targetFps = 30;
    private int crf = 23;
    private String preset = "veryfast";
    private String audioBitrate = "96k";
    private String bucket = "videos";
    private boolean publicBucket = true;
    private long signedUrlExpireSeconds = 3600;
    private Hls hls = new Hls();
    private Async async = new Async();
    private Process process = new Process();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxHeight() { return maxHeight; }
    public void setMaxHeight(int maxHeight) { this.maxHeight = maxHeight; }

    public int getTargetFps() { return targetFps; }
    public void setTargetFps(int targetFps) { this.targetFps = targetFps; }

    public int getCrf() { return crf; }
    public void setCrf(int crf) { this.crf = crf; }

    public String getPreset() { return preset; }
    public void setPreset(String preset) { this.preset = preset; }

    public String getAudioBitrate() { return audioBitrate; }
    public void setAudioBitrate(String audioBitrate) { this.audioBitrate = audioBitrate; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public boolean isPublicBucket() { return publicBucket; }
    public void setPublicBucket(boolean publicBucket) { this.publicBucket = publicBucket; }

    public long getSignedUrlExpireSeconds() { return signedUrlExpireSeconds; }
    public void setSignedUrlExpireSeconds(long signedUrlExpireSeconds) { this.signedUrlExpireSeconds = signedUrlExpireSeconds; }

    public Hls getHls() { return hls; }
    public void setHls(Hls hls) { this.hls = hls; }

    public Async getAsync() { return async; }
    public void setAsync(Async async) { this.async = async; }

    public Process getProcess() { return process; }
    public void setProcess(Process process) { this.process = process; }

    public static class Hls {
        private boolean enabled = false;
        private int segmentSeconds = 6;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getSegmentSeconds() { return segmentSeconds; }
        public void setSegmentSeconds(int segmentSeconds) { this.segmentSeconds = segmentSeconds; }
    }

    public static class Async {
        private int poolSize = 2;

        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    }

    public static class Process {
        private int timeoutSeconds = 1800;

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}
