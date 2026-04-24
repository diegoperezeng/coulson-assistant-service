package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.usecase.ParticipantUseCase;
import com.yellowleaf.coulson.domain.model.Participant;
import com.yellowleaf.coulson.interfaces.rest.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/participants/voice-sample")
@Tag(name = "Participants", description = "Participant management")
public class ParticipantVoiceSampleController {

    private final ParticipantUseCase participantUseCase;

    public ParticipantVoiceSampleController(ParticipantUseCase participantUseCase) {
        this.participantUseCase = participantUseCase;
    }

    @Operation(summary = "Download the voice sample of a participant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Voice sample file"),
            @ApiResponse(responseCode = "404", description = "Participant not found or no voice sample",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<byte[]> download(
            @Parameter(description = "Participant ID", required = true) @RequestParam UUID id) {

        Participant participant = participantUseCase.findById(id);
        byte[] content = participantUseCase.downloadVoiceSample(id);

        String filename = participant.getVoiceSampleKey() != null
                ? participant.getVoiceSampleKey()
                : id + ".bin";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentType(resolveMediaType(filename));

        return ResponseEntity.ok().headers(headers).body(content);
    }

    private MediaType resolveMediaType(String filename) {
        if (filename == null) return MediaType.APPLICATION_OCTET_STREAM;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".m4a")) return MediaType.parseMediaType("audio/x-m4a");
        if (lower.endsWith(".mp3")) return MediaType.parseMediaType("audio/mpeg");
        if (lower.endsWith(".wav")) return MediaType.parseMediaType("audio/wav");
        if (lower.endsWith(".ogg")) return MediaType.parseMediaType("audio/ogg");
        if (lower.endsWith(".flac")) return MediaType.parseMediaType("audio/flac");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
