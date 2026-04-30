package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.usecase.ParticipantPresentationUseCase;
import com.yellowleaf.coulson.interfaces.rest.dto.ParticipantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/participants/{id}/presentation-voice")
@Tag(name = "Participant Presentation Voice", description = "Generate and manage participant presentation audio via ElevenLabs")
public class ParticipantPresentationController {

    private final ParticipantPresentationUseCase useCase;

    public ParticipantPresentationController(ParticipantPresentationUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Generate presentation voice",
            description = "Clones the participant voice via ElevenLabs and generates a presentation audio with 'Eu sou {name} e eu sou um participante.'.")
    @PostMapping
    public ResponseEntity<ParticipantResponse> generate(
            @Parameter(description = "Participant ID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ParticipantResponse.from(useCase.generate(id)));
    }

    @Operation(summary = "Download presentation audio",
            description = "Downloads the generated presentation mp3 for the participant.")
    @GetMapping
    public ResponseEntity<byte[]> download(
            @Parameter(description = "Participant ID", required = true) @PathVariable UUID id) {
        try {
            byte[] audio = useCase.download(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentDispositionFormData("attachment", "presentation-" + id + ".mp3");
            return ResponseEntity.ok().headers(headers).body(audio);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete presentation audio",
            description = "Removes the presentation audio from storage and clears the participant reference.")
    @DeleteMapping
    public ResponseEntity<ParticipantResponse> delete(
            @Parameter(description = "Participant ID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(ParticipantResponse.from(useCase.delete(id)));
    }
}
