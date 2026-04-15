package com.yellowleaf.coulson.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yellowleaf.coulson.domain.model.SpeakerMapping;
import com.yellowleaf.coulson.domain.model.TranscriptionSegment;
import com.yellowleaf.coulson.domain.port.SpeakerResolutionPort;
import com.yellowleaf.coulson.infrastructure.config.WebhookProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpSpeakerResolutionAdapter implements SpeakerResolutionPort {

    private static final Logger log = LoggerFactory.getLogger(HttpSpeakerResolutionAdapter.class);
    private static final String WEBHOOK_TOKEN_HEADER = "webhook-token";

    private final RestTemplate restTemplate;
    private final WebhookProperties webhookProperties;
    private final ObjectMapper objectMapper;

    public HttpSpeakerResolutionAdapter(
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

        List<Map<String, Object>> segmentList = segments.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("speaker", s.getSpeaker());
                    map.put("start", s.getStart());
                    map.put("end", s.getEnd());
                    map.put("text", s.getText());
                    return map;
                })
                .toList();

        Map<String, Object> body = Map.of("segments", segmentList);

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

        List<SpeakerMapping> mappings = parseMappings(response.getBody());
        log.info("Resolved {} speaker mapping(s)", mappings.size());
        return mappings;
    }

    private List<SpeakerMapping> parseMappings(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode array = root.isArray() ? root : root.get(0);

            if (array == null || !array.isArray()) {
                log.warn("Unexpected speakers response format, returning empty mappings");
                return List.of();
            }

            List<SpeakerMapping> mappings = new ArrayList<>();
            for (JsonNode node : array) {
                String speaker = node.has("speaker") ? node.get("speaker").asText() : null;
                String name = node.has("name") ? node.get("name").asText() : null;
                if (speaker != null && name != null) {
                    mappings.add(new SpeakerMapping(speaker, name));
                }
            }
            return mappings;
        } catch (Exception e) {
            log.error("Failed to parse speakers response: {}", responseBody);
            return List.of();
        }
    }
}
