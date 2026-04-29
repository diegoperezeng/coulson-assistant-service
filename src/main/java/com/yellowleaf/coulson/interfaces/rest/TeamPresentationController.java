package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.usecase.TeamPresentationUseCase;
import com.yellowleaf.coulson.interfaces.rest.dto.TeamResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams/{id}/presentation-audio")
@Tag(name = "Team Presentation Audio", description = "Generate and manage the team presentation audio by concatenating member presentation voices")
public class TeamPresentationController {

    private final TeamPresentationUseCase useCase;

    public TeamPresentationController(TeamPresentationUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Generate team presentation audio",
            description = "Concatenates all team member presentation audios into a single team presentation mp3.")
    @PostMapping
    public ResponseEntity<TeamResponse> generate(
            @Parameter(description = "Team ID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(TeamResponse.from(useCase.generate(id)));
    }

    @Operation(summary = "Download team presentation audio",
            description = "Downloads the generated team presentation mp3.")
    @GetMapping(produces = "audio/mpeg")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "Team ID", required = true) @PathVariable UUID id) {
        byte[] audio = useCase.download(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentDispositionFormData("attachment", "team-presentation-" + id + ".mp3");
        return ResponseEntity.ok().headers(headers).body(audio);
    }

    @Operation(summary = "Delete team presentation audio",
            description = "Removes the team presentation audio from storage and clears the team reference.")
    @DeleteMapping
    public ResponseEntity<TeamResponse> delete(
            @Parameter(description = "Team ID", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(TeamResponse.from(useCase.delete(id)));
    }
}
