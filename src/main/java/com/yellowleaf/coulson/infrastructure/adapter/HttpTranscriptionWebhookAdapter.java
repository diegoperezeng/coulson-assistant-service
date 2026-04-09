package com.yellowleaf.coulson.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yellowleaf.coulson.domain.model.AudioChunk;
import com.yellowleaf.coulson.domain.model.Transcription;
import com.yellowleaf.coulson.domain.model.TranscriptionSegment;
import com.yellowleaf.coulson.domain.port.TranscriptionWebhookPort;
import com.yellowleaf.coulson.infrastructure.config.WebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

public class HttpTranscriptionWebhookAdapter implements TranscriptionWebhookPort {

    private static final Logger log = LoggerFactory.getLogger(HttpTranscriptionWebhookAdapter.class);
    private static final String WEBHOOK_TOKEN_HEADER = "webhook-token";
    private static final String FILE_FIELD = "file";

    private final RestTemplate restTemplate;
    private final WebhookProperties webhookProperties;
    private final ObjectMapper objectMapper;

    public HttpTranscriptionWebhookAdapter(
            RestTemplate restTemplate,
            WebhookProperties webhookProperties,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.webhookProperties = webhookProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Transcription transcribe(AudioChunk audioChunk) {
        log.info("Sending merged audio [videoId={}] to transcription webhook", audioChunk.getVideoId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(WEBHOOK_TOKEN_HEADER, webhookProperties.getToken());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(FILE_FIELD, new FileSystemResource(audioChunk.getFilePath()));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(
                    webhookProperties.getTranscribeUrl(),
                    request,
                    String.class
            );
        } catch (HttpStatusCodeException ex) {
            log.error("Transcription webhook error [videoId={}] status={} body={}",
                    audioChunk.getVideoId(),
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString());
            throw ex;
        }

        Transcription transcription = parseResponse(audioChunk.getVideoId(), response.getBody());
        log.info("Transcription received [videoId={}] segments={}",
                audioChunk.getVideoId(), transcription.getSegments().size());

        return transcription;
    }

    private Transcription parseResponse(String videoId, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return new Transcription(0, videoId, "", List.of());
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode target = root.isArray() ? root.get(0) : root;

            if (target == null) {
                return new Transcription(0, videoId, responseBody, List.of());
            }

            String text = target.has("text") ? target.get("text").asText() : responseBody;
            List<TranscriptionSegment> segments = parseSegments(target);

            return new Transcription(0, videoId, text, segments);

        } catch (Exception e) {
            log.debug("Failed to parse transcription JSON, returning raw body");
            return new Transcription(0, videoId, responseBody, List.of());
        }
    }

    private List<TranscriptionSegment> parseSegments(JsonNode root) {
        List<TranscriptionSegment> segments = new ArrayList<>();

        JsonNode segmentsNode = root.get("segments");
        if (segmentsNode == null || !segmentsNode.isArray()) {
            return segments;
        }

        for (JsonNode node : segmentsNode) {
            String speaker = node.has("speaker") ? node.get("speaker").asText() : "unknown";
            double start = node.has("start") ? node.get("start").asDouble() : 0.0;
            double end = node.has("end") ? node.get("end").asDouble() : 0.0;
            String text = node.has("text") ? node.get("text").asText() : "";
            segments.add(new TranscriptionSegment(speaker, start, end, text));
        }

        return segments;
    }
}
