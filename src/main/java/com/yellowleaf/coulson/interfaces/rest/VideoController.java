package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.command.ProcessVideoCommand;
import com.yellowleaf.coulson.application.result.TranscriptionResult;
import com.yellowleaf.coulson.application.usecase.ProcessVideoUseCase;
import com.yellowleaf.coulson.interfaces.rest.dto.ErrorResponse;
import com.yellowleaf.coulson.interfaces.rest.dto.TranscriptionResponse;
import com.yellowleaf.coulson.interfaces.rest.dto.TranscriptionSegmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/videos")
@Tag(name = "Videos", description = "Video upload and transcription")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    private final ProcessVideoUseCase processVideoUseCase;

    public VideoController(ProcessVideoUseCase processVideoUseCase) {
        this.processVideoUseCase = processVideoUseCase;
    }

    @Operation(
            summary = "Transcribe a video file",
            description = """
                    Receives a binary video file and returns its full transcription.
                    
                    If the file exceeds 200 MB, it is automatically split into smaller chunks \
                    using FFmpeg (stream copy, no re-encoding). Each chunk is sent sequentially \
                    to the configured transcription webhook, and all partial transcriptions are \
                    joined into a single text in the response.
                    
                    Supported formats: any container supported by FFmpeg (mp4, mov, mkv, avi, webm, etc).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transcription completed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TranscriptionResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Empty or missing file",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "File exceeds the maximum allowed upload size configured in the server",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Transcription webhook is unreachable or returned an error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error during video processing or FFmpeg execution",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptionResponse> transcribe(
            @Parameter(
                    description = "Video file to be transcribed. Files larger than 200 MB are split automatically.",
                    required = true
            )
            @RequestParam("file") MultipartFile file) {

        log.info("Received transcription request for file: {} size: {}MB",
                file.getOriginalFilename(),
                file.getSize() / (1024 * 1024));

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ProcessVideoCommand command = new ProcessVideoCommand(file);
        TranscriptionResult result = processVideoUseCase.execute(command);

        String videoId = result.getVideoId();
        String transcription = result.getTranscription();
        int totalChunks = result.getTotalChunks();

        log.info("Transcription: {} Video ID: {} Total Chunks: {}", transcription, videoId, totalChunks);

        List<TranscriptionSegmentDto> segmentDtos = result.getSegments().stream()
                .map(s -> new TranscriptionSegmentDto(s.getSpeaker(), s.getStart(), s.getEnd(), s.getText()))
                .toList();

        return ResponseEntity.ok(new TranscriptionResponse(
                videoId,
                transcription,
                totalChunks,
                segmentDtos
        ));
    }
}
