package com.yellowleaf.coulson.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yellowleaf.coulson.domain.port.VideoAssetStoragePort;
import com.yellowleaf.coulson.infrastructure.config.SupabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SupabaseVideoAssetStorageAdapter implements VideoAssetStoragePort {

    private static final Logger log = LoggerFactory.getLogger(SupabaseVideoAssetStorageAdapter.class);

    private final RestTemplate restTemplate;
    private final SupabaseProperties supabaseProperties;
    private final ObjectMapper objectMapper;

    public SupabaseVideoAssetStorageAdapter(RestTemplate restTemplate,
                                            SupabaseProperties supabaseProperties,
                                            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.supabaseProperties = supabaseProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String upload(String bucket, String key, Path filePath, String contentType) {
        String url = supabaseProperties.getUrl() + "/storage/v1/object/" + bucket + "/" + key;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseProperties.getServiceRoleKey());
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<FileSystemResource> entity = new HttpEntity<>(new FileSystemResource(filePath), headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        long sizeMb;
        try {
            sizeMb = Files.size(filePath) / (1024 * 1024);
        } catch (IOException e) {
            sizeMb = -1;
        }
        log.info("Video asset uploaded: {}/{} size={}MB", bucket, key, sizeMb);
        return key;
    }

    @Override
    public String getPublicUrl(String bucket, String key) {
        return supabaseProperties.getUrl() + "/storage/v1/object/public/" + bucket + "/" + key;
    }

    @Override
    public String getSignedUrl(String bucket, String key, long expiresInSeconds) {
        String url = supabaseProperties.getUrl() + "/storage/v1/object/sign/" + bucket + "/" + key;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseProperties.getServiceRoleKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"expiresIn\": " + expiresInSeconds + "}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        String response = restTemplate.postForObject(url, entity, String.class);
        try {
            JsonNode root = objectMapper.readTree(response);
            String signedPath = root.path("signedURL").asText();
            return supabaseProperties.getUrl() + signedPath;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse signed URL response for: " + bucket + "/" + key, e);
        }
    }
}
