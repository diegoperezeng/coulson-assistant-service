package com.yellowleaf.coulson.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A speaker segment within the transcription")
public class TranscriptionSegmentDto {

    @Schema(description = "Speaker identifier", example = "speaker_0")
    private final String speaker;

    @Schema(description = "Start time of the segment in seconds", example = "0.12")
    private final double start;

    @Schema(description = "End time of the segment in seconds", example = "3.45")
    private final double end;

    @Schema(description = "Text spoken in this segment", example = "Hello, how are you?")
    private final String text;

    public TranscriptionSegmentDto(String speaker, double start, double end, String text) {
        this.speaker = speaker;
        this.start = start;
        this.end = end;
        this.text = text;
    }

    public String getSpeaker() {
        return speaker;
    }

    public double getStart() {
        return start;
    }

    public double getEnd() {
        return end;
    }

    public String getText() {
        return text;
    }
}
