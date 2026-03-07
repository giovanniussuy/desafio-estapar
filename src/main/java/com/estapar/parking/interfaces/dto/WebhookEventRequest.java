package com.estapar.parking.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;

public record WebhookEventRequest(
    @JsonProperty("license_plate") @NotBlank String licensePlate,
    @JsonProperty("entry_time") Instant entryTime,
    @JsonProperty("exit_time") Instant exitTime,
    BigDecimal lat,
    BigDecimal lng,
    @JsonProperty("event_type") @NotBlank String eventType
) {
}
