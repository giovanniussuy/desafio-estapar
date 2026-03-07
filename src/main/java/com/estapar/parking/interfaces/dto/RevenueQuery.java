package com.estapar.parking.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RevenueQuery(
    @NotNull LocalDate date,
    @NotBlank String sector
) {
}
