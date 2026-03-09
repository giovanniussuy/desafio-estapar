package com.estapar.parking.interfaces.error;

import com.estapar.parking.interfaces.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn(
            "domain_exception traceId={} method={} path={} message={}",
            traceId,
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage()
        );
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), Instant.now(), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        String traceId = resolveTraceId(request);
        String details = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + "=" + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        log.warn(
            "validation_exception traceId={} method={} path={} errors={}",
            traceId,
            request.getMethod(),
            request.getRequestURI(),
            details
        );
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request payload", Instant.now(), traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error(
            "unhandled_exception traceId={} method={} path={}",
            traceId,
            request.getMethod(),
            request.getRequestURI(),
            ex
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Unexpected server error", Instant.now(), traceId));
    }

    private String resolveTraceId(HttpServletRequest request) {
        Object attribute = request.getAttribute(TraceIdFilter.TRACE_ID_KEY);
        if (attribute != null) {
            return attribute.toString();
        }
        String mdcTraceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);
        if (mdcTraceId != null && !mdcTraceId.isBlank()) {
            return mdcTraceId;
        }
        String headerTraceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        if (headerTraceId != null && !headerTraceId.isBlank()) {
            return headerTraceId;
        }
        return "unknown";
    }
}
