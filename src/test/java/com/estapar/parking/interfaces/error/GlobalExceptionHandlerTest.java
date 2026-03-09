package com.estapar.parking.interfaces.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.estapar.parking.interfaces.TraceIdFilter;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    void shouldResolveTraceIdFromRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");
        request.setAttribute(TraceIdFilter.TRACE_ID_KEY, "trace-attr");

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(new DomainException("bad"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("trace-attr", response.getBody().traceId());
    }

    @Test
    void shouldResolveTraceIdFromMdcWhenAttributeIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");
        MDC.put(TraceIdFilter.TRACE_ID_KEY, "trace-mdc");

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(new DomainException("bad"), request);

        assertEquals("trace-mdc", response.getBody().traceId());
    }

    @Test
    void shouldResolveTraceIdFromHeaderWhenMdcIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-header");

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(new DomainException("bad"), request);

        assertEquals("trace-header", response.getBody().traceId());
    }

    @Test
    void shouldFallbackToUnknownTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("unknown", response.getBody().traceId());
    }

    @Test
    void shouldFallbackToUnknownWhenMdcAndHeaderAreBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/any");
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "   ");
        MDC.put(TraceIdFilter.TRACE_ID_KEY, "");

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(new DomainException("bad"), request);

        assertEquals("unknown", response.getBody().traceId());
    }

    @Test
    void shouldHandleValidationException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/webhook");
        request.setAttribute(TraceIdFilter.TRACE_ID_KEY, "trace-validation");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "licensePlate", "must not be blank"));

        Method method = DummyValidationTarget.class.getDeclaredMethod("handle", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request payload", response.getBody().message());
        assertEquals("trace-validation", response.getBody().traceId());
    }

    static class DummyValidationTarget {
        @SuppressWarnings("unused")
        void handle(String value) {
        }
    }
}
