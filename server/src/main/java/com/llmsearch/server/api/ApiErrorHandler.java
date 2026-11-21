package com.llmsearch.server.api;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.llmsearch.server.index.IndexingInProgressException;

@RestControllerAdvice
public class ApiErrorHandler {
    public record ErrorResponse(String status, String error) {}

    private ResponseEntity<ErrorResponse> error(HttpStatus code, Exception e) {
        return ResponseEntity.status(code).body(new ErrorResponse("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(IndexingInProgressException.class)
    public ResponseEntity<ErrorResponse> conflict(IndexingInProgressException e) {
        return error(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> ioError(IOException e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> serverError(IllegalStateException e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
}
