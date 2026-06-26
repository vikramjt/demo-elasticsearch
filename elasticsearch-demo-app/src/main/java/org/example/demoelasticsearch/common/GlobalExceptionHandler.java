package org.example.demoelasticsearch.common;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }

    @ExceptionHandler(ElasticsearchException.class)
    public ResponseEntity<ApiErrorResponse> handleElasticsearchException(ElasticsearchException exception) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), exception.getMessage()));
    }
}
