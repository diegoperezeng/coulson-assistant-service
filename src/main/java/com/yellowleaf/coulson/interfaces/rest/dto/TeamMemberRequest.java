package com.yellowleaf.coulson.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to add or update a team member")
public class TeamMemberRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Member name", example = "Matheus")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
