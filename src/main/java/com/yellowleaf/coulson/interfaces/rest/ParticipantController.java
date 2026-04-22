package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.usecase.ParticipantUseCase;
import com.yellowleaf.coulson.interfaces.rest.dto.ErrorResponse;
import com.yellowleaf.coulson.interfaces.rest.dto.ParticipantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/participants")
@Tag(name = "Participants", description = "Participant management")
public class ParticipantController {

    private final ParticipantUseCase participantUseCase;

    public ParticipantController(ParticipantUseCase participantUseCase) {
        this.participantUseCase = participantUseCase;
    }

    @Operation(summary = "Register a participant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participant registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParticipantResponse> create(
            @Parameter(description = "Full name", required = true) @RequestParam String name,
            @Parameter(description = "Job role (default: indefinido)") @RequestParam(required = false) String role,
            @Parameter(description = "Voice sample audio file") @RequestPart(required = false) MultipartFile voiceSample)
            throws IOException {

        byte[] bytes = voiceSample != null ? voiceSample.getBytes() : null;
        String contentType = voiceSample != null ? voiceSample.getContentType() : null;
        String extension = resolveExtension(voiceSample);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ParticipantResponse.from(participantUseCase.create(name, role, bytes, contentType, extension)));
    }

    @Operation(summary = "List participants or find by ID",
            description = "Returns all participants when no ID is provided. Returns a single participant when ID is provided.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participants returned"),
            @ApiResponse(responseCode = "404", description = "Participant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<?> get(
            @Parameter(description = "Participant ID (optional — omit to list all)")
            @RequestParam(required = false) UUID id) {
        if (id != null) {
            return ResponseEntity.ok(ParticipantResponse.from(participantUseCase.findById(id)));
        }
        return ResponseEntity.ok(participantUseCase.findAll().stream()
                .map(ParticipantResponse::from).toList());
    }

    @Operation(summary = "Update a participant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant updated"),
            @ApiResponse(responseCode = "404", description = "Participant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(params = "id", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParticipantResponse> update(
            @Parameter(description = "Participant ID", required = true) @RequestParam UUID id,
            @Parameter(description = "Full name") @RequestParam(required = false) String name,
            @Parameter(description = "Job role") @RequestParam(required = false) String role,
            @Parameter(description = "New voice sample audio file") @RequestPart(required = false) MultipartFile voiceSample)
            throws IOException {

        byte[] bytes = voiceSample != null ? voiceSample.getBytes() : null;
        String contentType = voiceSample != null ? voiceSample.getContentType() : null;
        String extension = resolveExtension(voiceSample);

        return ResponseEntity.ok(ParticipantResponse.from(
                participantUseCase.update(id, name, role, bytes, contentType, extension)));
    }

    @Operation(summary = "Delete a participant")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participant deleted"),
            @ApiResponse(responseCode = "404", description = "Participant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(params = "id")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Participant ID", required = true) @RequestParam UUID id) {
        participantUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String resolveExtension(MultipartFile file) {
        if (file == null || file.getOriginalFilename() == null) return "bin";
        String filename = file.getOriginalFilename();
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "bin";
    }
}
