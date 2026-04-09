package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.model.VideoChunk;
import com.yellowleaf.coulson.domain.port.AudioExtractorWebhookPort;
import com.yellowleaf.coulson.infrastructure.config.VideoProperties;
import com.yellowleaf.coulson.infrastructure.config.WebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpAudioExtractorWebhookAdapter implements AudioExtractorWebhookPort {

    private static final Logger log = LoggerFactory.getLogger(HttpAudioExtractorWebhookAdapter.class);
    private static final String WEBHOOK_TOKEN_HEADER = "webhook-token";
    private static final String FILE_FIELD = "file";

    private final RestTemplate restTemplate;
    private final WebhookProperties webhookProperties;
    private final VideoProperties videoProperties;

    public HttpAudioExtractorWebhookAdapter(
            RestTemplate restTemplate,
            WebhookProperties webhookProperties,
            VideoProperties videoProperties) {
        this.restTemplate = restTemplate;
        this.webhookProperties = webhookProperties;
        this.videoProperties = videoProperties;
    }

    @Override
    public AudioChunk extract(VideoChunk videoChunk) {
        log.info("Extracting audio from chunk {} [videoId={}]", videoChunk.getIndex(), videoChunk.getVideoId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(WEBHOOK_TOKEN_HEADER, webhookProperties.getToken());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(FILE_FIELD, new FileSystemResource(videoChunk.getFilePath()));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.postForEntity(
                    webhookProperties.getExtractSoundUrl(),
                    request,
                    byte[].class
            );
        } catch (HttpStatusCodeException ex) {
            log.error("Extract sound webhook error for chunk {} [videoId={}] status={} body={}",
                    videoChunk.getIndex(),
                    videoChunk.getVideoId(),
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString());
            throw ex;
        }

        byte[] audioBytes = response.getBody();
        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalStateException(
                    "Empty audio response for chunk " + videoChunk.getIndex() + " [videoId=" + videoChunk.getVideoId() + "]");
        }

        Path audioPath = saveAudioChunk(videoChunk, audioBytes);
        log.info("Audio extracted for chunk {} [videoId={}] size={}KB",
                videoChunk.getIndex(), videoChunk.getVideoId(), audioBytes.length / 1024);

        return new AudioChunk(videoChunk.getIndex(), videoChunk.getVideoId(), audioPath, 0.0);
    }

    private Path saveAudioChunk(VideoChunk videoChunk, byte[] audioBytes) {
        try {
            Path tempDir = Paths.get(videoProperties.getTempDir());
            Files.createDirectories(tempDir);

            String fileName = videoChunk.getVideoId() + "-audio-" + videoChunk.getIndex() + ".mp3";
            Path audioPath = tempDir.resolve(fileName);
            Files.write(audioPath, audioBytes);

            return audioPath;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save audio chunk " + videoChunk.getIndex(), e);
        }
    }
}
