package com.yellowleaf.coulson.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    private String url;
    private String externalUrl;
    private String serviceRoleKey;
    private Storage storage = new Storage();

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getExternalUrl() { return externalUrl != null ? externalUrl : url; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    public String getServiceRoleKey() { return serviceRoleKey; }
    public void setServiceRoleKey(String serviceRoleKey) { this.serviceRoleKey = serviceRoleKey; }

    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }

    public static class Storage {

        private String voiceSamplesBucket = "voice-samples";
        private String presentationAudioBucket = "presentation-audio";

        public String getVoiceSamplesBucket() { return voiceSamplesBucket; }
        public void setVoiceSamplesBucket(String voiceSamplesBucket) { this.voiceSamplesBucket = voiceSamplesBucket; }

        public String getPresentationAudioBucket() { return presentationAudioBucket; }
        public void setPresentationAudioBucket(String presentationAudioBucket) { this.presentationAudioBucket = presentationAudioBucket; }
    }
}
