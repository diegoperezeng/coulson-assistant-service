package com.yellowleaf.coulson.infrastructure.adapter;

import com.yellowleaf.coulson.domain.port.StoragePort;
import com.yellowleaf.coulson.infrastructure.config.SupabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class SupabaseStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageAdapter.class);

    private final RestTemplate restTemplate;
    private final SupabaseProperties supabaseProperties;

    public SupabaseStorageAdapter(RestTemplate restTemplate, SupabaseProperties supabaseProperties) {
        this.restTemplate = restTemplate;
        this.supabaseProperties = supabaseProperties;
    }

    @Override
    public String upload(String bucket, String objectPath, byte[] content, String contentType) {
        String url = supabaseProperties.getUrl() + "/storage/v1/object/" + bucket + "/" + objectPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseProperties.getServiceRoleKey());
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> entity = new HttpEntity<>(content, headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        log.info("Voice sample uploaded: {}/{}", bucket, objectPath);
        return objectPath;
    }

    @Override
    public byte[] download(String bucket, String objectPath) {
        String url = supabaseProperties.getUrl() + "/storage/v1/object/" + bucket + "/" + objectPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseProperties.getServiceRoleKey());

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class).getBody();
    }

    @Override
    public void delete(String bucket, String objectPath) {
        String url = supabaseProperties.getUrl() + "/storage/v1/object/" + bucket + "/" + objectPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(supabaseProperties.getServiceRoleKey());

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            log.info("Voice sample deleted: {}/{}", bucket, objectPath);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Voice sample not found for deletion: {}/{}", bucket, objectPath);
        }
    }
}
