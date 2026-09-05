package com.shubham.matrimony.shubham_matrimony_biodata.exception;

import com.shubham.matrimony.shubham_matrimony_biodata.dto.WarningCategory;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an input fails Request Security, File Signature, or
 * Local Sanity Checks
 * in the Input Gate prior to calling external services or the deterministic
 * parser.
 */
@Getter
public class InputGateException extends RuntimeException {

    private final HttpStatus status;
    private final WarningCategory category;

    public InputGateException(HttpStatus status, WarningCategory category, String message) {
        super(message);
        this.status = status;
        this.category = category;
    }

    public InputGateException(HttpStatus status, WarningCategory category, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.category = category;
    }
}
