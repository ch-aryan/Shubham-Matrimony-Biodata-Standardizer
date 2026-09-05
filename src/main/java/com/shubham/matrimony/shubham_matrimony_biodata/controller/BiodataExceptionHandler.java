package com.shubham.matrimony.shubham_matrimony_biodata.controller;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseResponse;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseStatus;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.ParseWarning;
import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import com.shubham.matrimony.shubham_matrimony_biodata.exception.InputGateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * Global exception handler providing structured JSON diagnostics adhering to
 * ParseResponse
 * and preventing unhandled 500 stack trace exposure.
 */
@Slf4j
@RestControllerAdvice
public class BiodataExceptionHandler {

        @ExceptionHandler(InputGateException.class)
        public ResponseEntity<ParseResponse> handleInputGateException(InputGateException ex) {
                log.warn("Input Gate rejected request: [{}] {}", ex.getCategory(), ex.getMessage());
                return ResponseEntity.status(ex.getStatus()).body(
                                ParseResponse.builder()
                                                .status(ParseStatus.REJECTED_INPUT)
                                                .profile(null)
                                                .warnings(List.of(
                                                                ParseWarning.builder()
                                                                                .category(ex.getCategory())
                                                                                .message(ex.getMessage())
                                                                                .build()))
                                                .build());
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ParseResponse> handleMaxSizeException(MaxUploadSizeExceededException ex) {
                log.warn("Upload size exceeded: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                                ParseResponse.builder()
                                                .status(ParseStatus.REJECTED_INPUT)
                                                .profile(null)
                                                .warnings(List.of(
                                                                ParseWarning.builder()
                                                                                .category(WarningCategory.LOW_INFORMATION_INPUT)
                                                                                .message("Uploaded file exceeds the maximum permitted limit of 10MB.")
                                                                                .build()))
                                                .build());
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ParseResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
                log.warn("Unsupported media type: {}", ex.getMessage());
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
                                ParseResponse.builder()
                                                .status(ParseStatus.REJECTED_INPUT)
                                                .profile(null)
                                                .warnings(List.of(
                                                                ParseWarning.builder()
                                                                                .category(WarningCategory.UNSUPPORTED_MEDIA_TYPE)
                                                                                .message("Media type is not supported: "
                                                                                                + ex.getContentType())
                                                                                .build()))
                                                .build());
        }

        @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
        public ResponseEntity<ParseResponse> handleValidationException(
                        org.springframework.web.bind.MethodArgumentNotValidException ex) {
                String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                                .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                                .filter(msg -> msg != null && !msg.isBlank())
                                .findFirst()
                                .orElse("Invalid request payload.");
                log.warn("Validation failed: {}", errorMsg);
                return ResponseEntity.badRequest().body(
                                ParseResponse.builder()
                                                .status(ParseStatus.REJECTED_INPUT)
                                                .profile(null)
                                                .warnings(List.of(
                                                                ParseWarning.builder()
                                                                                .category(WarningCategory.LOW_INFORMATION_INPUT)
                                                                                .message(errorMsg)
                                                                                .build()))
                                                .build());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ParseResponse> handleIllegalArgument(IllegalArgumentException ex) {
                log.warn("Invalid argument: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(
                                ParseResponse.builder()
                                                .status(ParseStatus.REJECTED_INPUT)
                                                .profile(null)
                                                .warnings(List.of(
                                                                ParseWarning.builder()
                                                                                .category(WarningCategory.LOW_INFORMATION_INPUT)
                                                                                .message(ex.getMessage())
                                                                                .build()))
                                                .build());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ParseResponse> handleGenericException(Exception ex) {
                log.error("Unhandled error during biodata processing", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ParseResponse.builder()
                                                .status(ParseStatus.REJECTED_INPUT)
                                                .profile(null)
                                                .warnings(List.of(
                                                                ParseWarning.builder()
                                                                                .category(WarningCategory.LOW_INFORMATION_INPUT)
                                                                                .message("An unexpected error occurred while processing the biodata: "
                                                                                                + ex.getMessage())
                                                                                .build()))
                                                .build());
        }
}
