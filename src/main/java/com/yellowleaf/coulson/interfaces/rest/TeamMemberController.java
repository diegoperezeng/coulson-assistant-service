package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.usecase.TeamMemberUseCase;
import com.yellowleaf.coulson.interfaces.rest.dto.ErrorResponse;
import com.yellowleaf.coulson.interfaces.rest.dto.TeamMemberRequest;
import com.yellowleaf.coulson.interfaces.rest.dto.TeamMemberResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/team-members")
@Tag(name = "Team Members", description = "Team member management")
public class TeamMemberController {

    private final TeamMemberUseCase teamMemberUseCase;

    public TeamMemberController(TeamMemberUseCase teamMemberUseCase) {
        this.teamMemberUseCase = teamMemberUseCase;
    }

    @Operation(summary = "Add a participant to a team")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participant added to team"),
            @ApiResponse(responseCode = "400", description = "Invalid request or participant already in team",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team or participant not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(params = "teamId")
    public ResponseEntity<TeamMemberResponse> add(
            @Parameter(description = "Team ID", required = true) @RequestParam UUID teamId,
            @Valid @RequestBody TeamMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TeamMemberResponse.from(teamMemberUseCase.add(teamId, request.getParticipantId())));
    }

    @Operation(summary = "List team members or find a specific one",
            description = "Returns all members across all teams when no parameters are provided. " +
                    "Returns members of a specific team when only teamId is provided. " +
                    "Returns a single member when both teamId and participantId are provided.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members returned"),
            @ApiResponse(responseCode = "404", description = "Team or member not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<TeamMemberResponse>> get(
            @Parameter(description = "Team ID (optional)") @RequestParam(required = false) UUID teamId,
            @Parameter(description = "Participant ID (optional — requires teamId)") @RequestParam(required = false) UUID participantId) {
        if (teamId != null && participantId != null) {
            return ResponseEntity.ok(List.of(
                    TeamMemberResponse.from(teamMemberUseCase.findById(teamId, participantId))));
        }
        if (teamId != null) {
            return ResponseEntity.ok(teamMemberUseCase.findAllByTeam(teamId).stream()
                    .map(TeamMemberResponse::from).toList());
        }
        return ResponseEntity.ok(teamMemberUseCase.findAll().stream()
                .map(TeamMemberResponse::from).toList());
    }

    @Operation(summary = "Remove a participant from a team")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participant removed from team"),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(params = {"teamId", "participantId"})
    public ResponseEntity<Void> remove(
            @Parameter(description = "Team ID", required = true) @RequestParam UUID teamId,
            @Parameter(description = "Participant ID", required = true) @RequestParam UUID participantId) {
        teamMemberUseCase.remove(teamId, participantId);
        return ResponseEntity.noContent().build();
    }
}
