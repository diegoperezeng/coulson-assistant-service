package com.yellowleaf.coulson.interfaces.rest;

import com.yellowleaf.coulson.interfaces.rest.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.UncheckedIOException;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse(413, "Payload Too Large", "File exceeds the maximum allowed upload size"));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleWebhookError(RestClientException ex) {
        log.error("Webhook call failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(502, "Bad Gateway", "Failed to communicate with transcription webhook"));
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<ErrorResponse> handleIOError(UncheckedIOException ex) {
        log.error("IO error during video processing", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", "File processing error: " + ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error during video processing", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
