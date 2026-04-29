package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.application.usecase.PersistVideoUseCase;
import com.yellowleaf.coulson.domain.model.VideoAsset;
import com.yellowleaf.coulson.interfaces.rest.dto.VideoAssetResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoAssetController {

    private final PersistVideoUseCase persistVideoUseCase;

    public VideoAssetController(PersistVideoUseCase persistVideoUseCase) {
        this.persistVideoUseCase = persistVideoUseCase;
    }

    @PostMapping(value = "/by-team/{teamId}/persist", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoAssetResponse> persist(
            @PathVariable UUID teamId,
            @RequestParam("file") MultipartFile file) {
        if (!persistVideoUseCase.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Video persistence is disabled. Set VIDEO_PERSIST_ENABLED=true to enable.");
        }
        persistVideoUseCase.persistManual(teamId, file);
        String refId = "manual-" + teamId;
        VideoAsset asset = persistVideoUseCase.findByRefId(refId)
                .orElseThrow(() -> new IllegalStateException("VideoAsset not found after submit for refId=" + refId));
        return ResponseEntity.accepted().body(toResponse(asset));
    }

    @GetMapping("/by-team/{teamId}")
    public ResponseEntity<List<VideoAssetResponse>> getByTeam(@PathVariable UUID teamId) {
        List<VideoAssetResponse> response = persistVideoUseCase.findByTeamId(teamId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private VideoAssetResponse toResponse(VideoAsset asset) {
        VideoAssetResponse resp = new VideoAssetResponse();
        resp.setId(asset.getId());
        resp.setRefId(asset.getRefId());
        resp.setTeamId(asset.getTeamId());
        resp.setTeamName(asset.getTeamName());
        resp.setStatus(asset.getStatus());
        resp.setDurationMs(asset.getDurationMs());
        resp.setWidth(asset.getWidth());
        resp.setHeight(asset.getHeight());
        resp.setFps(asset.getFps());
        resp.setPlaybackSizeBytes(asset.getPlaybackSizeBytes());
        resp.setErrorMessage(asset.getErrorMessage());
        resp.setCreatedAt(asset.getCreatedAt());
        resp.setUpdatedAt(asset.getUpdatedAt());

        String url = persistVideoUseCase.resolveUrl(asset);
        resp.setPlaybackUrl(url);

        return resp;
    }
}
