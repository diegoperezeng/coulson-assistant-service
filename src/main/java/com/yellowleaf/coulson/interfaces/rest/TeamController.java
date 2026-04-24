package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.usecase.TeamUseCase;
import com.yellowleaf.coulson.interfaces.rest.dto.ErrorResponse;
import com.yellowleaf.coulson.interfaces.rest.dto.TeamRequest;
import com.yellowleaf.coulson.interfaces.rest.dto.TeamResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Meeting team management")
public class TeamController {

    private final TeamUseCase teamUseCase;

    public TeamController(TeamUseCase teamUseCase) {
        this.teamUseCase = teamUseCase;
    }

    @Operation(summary = "Create a team")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Team created"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TeamResponse.from(teamUseCase.create(request.getName())));
    }

    @Operation(summary = "List teams or find by ID",
            description = "Returns all teams when no ID is provided. Returns a single team (with members) when ID is provided.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teams returned"),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<?> get(
            @Parameter(description = "Team ID (optional — omit to list all)")
            @RequestParam(required = false) UUID id) {
        if (id != null) {
            return ResponseEntity.ok(TeamResponse.from(teamUseCase.findById(id)));
        }
        return ResponseEntity.ok(teamUseCase.findAll().stream().map(TeamResponse::from).toList());
    }

    @Operation(summary = "Delete a team and all its members")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Team deleted"),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(params = "id")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Team ID", required = true)
            @RequestParam UUID id) {
        teamUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
