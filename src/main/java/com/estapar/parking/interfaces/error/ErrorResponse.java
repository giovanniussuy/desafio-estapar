package com.estapar.parking.interfaces.error;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp, String traceId) {
}
