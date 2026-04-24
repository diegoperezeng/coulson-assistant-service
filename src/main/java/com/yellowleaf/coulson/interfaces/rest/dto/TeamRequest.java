package com.yellowleaf.coulson.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to create a team")
public class TeamRequest {

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Team name", example = "Time de Produto")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
