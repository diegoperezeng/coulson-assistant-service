package com.yellowleaf.coulson.infrastructure.adapter.elevenlabs;

import feign.form.FormData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;

@FeignClient(
        name = "elevenlabs",
        url = "${elevenlabs.base-url}",
        configuration = ElevenLabsFeignConfig.class
)
public interface ElevenLabsFeignClient {

    @PostMapping(value = "/v1/voices/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ElevenLabsVoiceResponse addVoice(
            @RequestHeader("xi-api-key") String apiKey,
            @RequestPart("name") String name,
            @RequestPart("files") FormData audioFile
    );

    @PostMapping(
            value = "/v1/text-to-speech/{voiceId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    byte[] textToSpeech(
            @RequestHeader("xi-api-key") String apiKey,
            @PathVariable("voiceId") String voiceId,
            @RequestBody ElevenLabsTtsRequest request
    );
}
