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
import org.springframework.web.bind.annotation.PutMapping;
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

    @Operation(summary = "Add a member to a team")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member added"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(params = "teamId")
    public ResponseEntity<TeamMemberResponse> add(
            @Parameter(description = "Team ID", required = true) @RequestParam UUID teamId,
            @Valid @RequestBody TeamMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TeamMemberResponse.from(teamMemberUseCase.add(teamId, request.getName())));
    }

    @Operation(summary = "List all members of a team")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members returned"),
            @ApiResponse(responseCode = "404", description = "Team not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(params = "teamId")
    public ResponseEntity<List<TeamMemberResponse>> findAllByTeam(
            @Parameter(description = "Team ID", required = true) @RequestParam UUID teamId) {
        return ResponseEntity.ok(teamMemberUseCase.findAllByTeam(teamId).stream()
                .map(TeamMemberResponse::from).toList());
    }

    @Operation(summary = "Find a specific member")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member found"),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(params = {"teamId", "memberId"})
    public ResponseEntity<TeamMemberResponse> findById(
            @Parameter(description = "Team ID", required = true) @RequestParam UUID teamId,
            @Parameter(description = "Member ID", required = true) @RequestParam UUID memberId) {
        return ResponseEntity.ok(TeamMemberResponse.from(teamMemberUseCase.findById(teamId, memberId)));
    }

    @Operation(summary = "Update a member name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member updated"),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(params = "memberId")
    public ResponseEntity<TeamMemberResponse> update(
            @Parameter(description = "Member ID", required = true) @RequestParam UUID memberId,
            @Valid @RequestBody TeamMemberRequest request) {
        return ResponseEntity.ok(TeamMemberResponse.from(teamMemberUseCase.update(memberId, request.getName())));
    }

    @Operation(summary = "Remove a member from a team")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed"),
            @ApiResponse(responseCode = "404", description = "Member not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(params = "memberId")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Member ID", required = true) @RequestParam UUID memberId) {
        teamMemberUseCase.delete(memberId);
        return ResponseEntity.noContent().build();
    }
}
