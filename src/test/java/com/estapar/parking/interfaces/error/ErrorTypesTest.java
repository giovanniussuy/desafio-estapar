package com.estapar.parking.interfaces.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ErrorTypesTest {

    @Test
    void shouldCreateDomainExceptionWithMessage() {
        DomainException ex = new DomainException("message");
        assertEquals("message", ex.getMessage());
    }

    @Test
    void shouldExposeErrorResponseFields() {
        Instant now = Instant.now();
        ErrorResponse response = new ErrorResponse("error", now);

        assertEquals("error", response.message());
        assertEquals(now, response.timestamp());
    }
}
