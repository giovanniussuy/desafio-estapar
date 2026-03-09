package com.estapar.parking.domain.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class SessionStatusTest {

    @Test
    void shouldExposeExpectedEnumValues() {
        assertArrayEquals(
            new SessionStatus[] {SessionStatus.ENTERED, SessionStatus.PARKED, SessionStatus.EXITED},
            SessionStatus.values()
        );
    }
}
