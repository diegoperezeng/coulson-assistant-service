package com.yellowleaf.coulson.infrastructure.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yellowleaf.coulson.domain.model.SpeakerMapping;
import com.yellowleaf.coulson.domain.model.TranscriptionSegment;
import com.yellowleaf.coulson.domain.port.SpeakerResolutionWebhookPort;
import com.yellowleaf.coulson.infrastructure.config.WebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class HttpSpeakerResolutionWebhookAdapter implements SpeakerResolutionWebhookPort {

    private static final Logger log = LoggerFactory.getLogger(HttpSpeakerResolutionWebhookAdapter.class);
    private static final String WEBHOOK_TOKEN_HEADER = "webhook-token";

    private final RestTemplate restTemplate;
    private final WebhookProperties webhookProperties;
    private final ObjectMapper objectMapper;

    public HttpSpeakerResolutionWebhookAdapter(
            RestTemplate restTemplate,
            WebhookProperties webhookProperties,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.webhookProperties = webhookProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SpeakerMapping> resolve(List<TranscriptionSegment> segments) {
        log.info("Resolving speaker names for {} segment(s)", segments.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(WEBHOOK_TOKEN_HEADER, webhookProperties.getToken());

        Map<String, Object> body = Map.of("segments", segments.stream()
                .map(s -> Map.of(
                        "speaker", s.getSpeaker(),
                        "start", s.getStart(),
                        "end", s.getEnd(),
                        "text", s.getText()
                ))
                .toList());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(
                    webhookProperties.getSpeakersUrl(),
                    request,
                    String.class
            );
        } catch (HttpStatusCodeException ex) {
            log.error("Speakers webhook error status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw ex;
        }

        return parseMappings(response.getBody());
    }

    private List<SpeakerMapping> parseMappings(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            log.warn("Empty response from speakers webhook, no name substitution will be applied");
            return List.of();
        }

        try {
            List<Map<String, String>> raw = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );

            List<SpeakerMapping> mappings = raw.stream()
                    .filter(m -> m.containsKey("speaker") && m.containsKey("name"))
                    .map(m -> new SpeakerMapping(m.get("speaker"), m.get("name")))
                    .toList();

            log.info("Received {} speaker mapping(s)", mappings.size());
            return mappings;

        } catch (Exception e) {
            log.error("Failed to parse speakers webhook response: {}", responseBody);
            return List.of();
        }
    }
}
