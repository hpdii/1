package com.donk.server.common;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Gom moi loi ve cung mot hinh dang JSON. Khong co lop nay thi moi loi se ra
 * hinh dang khac nhau va client khong parse duoc.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req);
    }

    /** Loi validate @Valid tren request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (message.isBlank()) {
            message = "Du lieu gui len khong hop le";
        }

        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    /**
     * File qua lon. Nho dat server.tomcat.max-swallow-size=-1 thi Tomcat moi doc
     * het body va tra ve day du thay vi cat ket noi giua chung.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex,
                                                   HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE,
                "File qua lon. Gioi han la 200MB.", req);
    }

    /**
     * Loi do thieu quyen. Phai khai bao rieng: neu de lot vao handler
     * Exception.class ben duoi thi se tra 500 thay vi 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex,
                                                 HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Ban khong co quyen thuc hien thao tac nay", req);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoHandlerFoundException ex,
                                                   HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Khong tim thay duong dan nay", req);
    }

    /** Luoi an toan cuoi cung — khong de lo stack trace ra ngoai. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        ex.printStackTrace();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Loi he thong: " + ex.getMessage(), req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest req) {
        ApiError body = new ApiError(
                LocalDateTime.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
