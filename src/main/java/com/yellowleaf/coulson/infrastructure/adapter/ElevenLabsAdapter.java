package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.port.TextToSpeechPort;
import com.yellowleaf.coulson.domain.port.VoiceCloningPort;
import com.yellowleaf.coulson.infrastructure.adapter.elevenlabs.ElevenLabsFeignClient;
import com.yellowleaf.coulson.infrastructure.adapter.elevenlabs.ElevenLabsTtsRequest;
import com.yellowleaf.coulson.infrastructure.adapter.elevenlabs.ElevenLabsVoiceSettings;
import com.yellowleaf.coulson.infrastructure.config.ElevenLabsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

public class ElevenLabsAdapter implements VoiceCloningPort, TextToSpeechPort {

    private static final Logger log = LoggerFactory.getLogger(ElevenLabsAdapter.class);

    private final ElevenLabsFeignClient feignClient;
    private final ElevenLabsProperties properties;

    public ElevenLabsAdapter(ElevenLabsFeignClient feignClient, ElevenLabsProperties properties) {
        this.feignClient = feignClient;
        this.properties = properties;
    }

    @Override
    public String createVoiceClone(String name, byte[] audioBytes, String audioFileName) {
        log.info("Creating voice clone for: {}", name);
        Resource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return audioFileName;
            }
        };
        String voiceId = feignClient.addVoice(properties.getApiKey(), name, audioResource).getVoiceId();
        log.info("Voice clone created [voiceId={}] for: {}", voiceId, name);
        return voiceId;
    }

    @Override
    public byte[] generateSpeech(String voiceId, String text) {
        log.info("Generating speech [voiceId={}] text length={}", voiceId, text.length());
        ElevenLabsVoiceSettings voiceSettings = new ElevenLabsVoiceSettings(
                properties.getStability(),
                properties.getSimilarityBoost(),
                properties.getStyle(),
                properties.getSpeed()
        );
        ElevenLabsTtsRequest request = new ElevenLabsTtsRequest(
                text,
                properties.getModelId(),
                "pt",
                voiceSettings
        );
        byte[] audioBytes = feignClient.textToSpeech(properties.getApiKey(), voiceId, request);
        log.info("Speech generated [voiceId={}] size={}KB", voiceId, audioBytes.length / 1024);
        return audioBytes;
    }
}
