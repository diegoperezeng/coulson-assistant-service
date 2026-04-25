package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.usecase.GetTranscriptionJobUseCase;
import com.yellowleaf.coulson.application.usecase.SubmitTranscriptionJobUseCase;
import com.yellowleaf.coulson.domain.model.TranscriptionJob;
import com.yellowleaf.coulson.interfaces.rest.dto.ErrorResponse;
import com.yellowleaf.coulson.interfaces.rest.dto.TranscriptionJobResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
@Tag(name = "Videos", description = "Video upload and transcription")
public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    private final SubmitTranscriptionJobUseCase submitUseCase;
    private final GetTranscriptionJobUseCase getUseCase;

    public VideoController(
            SubmitTranscriptionJobUseCase submitUseCase,
            GetTranscriptionJobUseCase getUseCase) {
        this.submitUseCase = submitUseCase;
        this.getUseCase = getUseCase;
    }

    @Operation(
            summary = "Submit a video for transcription",
            description = """
                    Accepts a video file, stores it and immediately returns a job ID.
                    The transcription runs asynchronously in the background.

                    If the file exceeds 200 MB, it is automatically split into smaller chunks \
                    using FFmpeg (stream copy, no re-encoding). Each chunk is sent sequentially \
                    to the configured transcription webhook, and all partial transcriptions are \
                    joined into a single text in the result.

                    Poll GET /api/v1/videos/transcribe/{jobId} to retrieve the result.

                    Supported formats: any container supported by FFmpeg (mp4, mov, mkv, avi, webm, etc).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Job accepted, transcription running in background",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TranscriptionJobResponse.class)
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
                    responseCode = "500",
                    description = "Internal error during file storage",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranscriptionJobResponse> transcribe(
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

        TranscriptionJob job = submitUseCase.submit(file);

        return ResponseEntity.accepted().body(toResponse(job));
    }

    @Operation(
            summary = "Get transcription job status and result",
            description = """
                    Returns the current status of a transcription job.

                    - **PROCESSING**: transcription is still running.
                    - **DONE**: transcription is complete. The `result` field contains the full output.
                    - **FAILED**: transcription failed. The `error` field contains the reason.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Job status returned",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TranscriptionJobResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Job not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/transcribe/{jobId}")
    public ResponseEntity<TranscriptionJobResponse> getJob(
            @Parameter(description = "Job ID returned by the POST endpoint", required = true)
            @PathVariable UUID jobId) {

        TranscriptionJob job = getUseCase.get(jobId);
        return ResponseEntity.ok(toResponse(job));
    }

    private TranscriptionJobResponse toResponse(TranscriptionJob job) {
        return TranscriptionJobResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus().name())
                .videoName(job.getVideoName())
                .error(job.getError())
                .result(job.getResultJson())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
