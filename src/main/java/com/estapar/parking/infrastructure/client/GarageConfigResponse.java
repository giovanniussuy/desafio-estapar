package com.estapar.parking.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record GarageConfigResponse(
    List<GarageSectorResponse> garage,
    List<GarageSpotResponse> spots
) {

    public record GarageSectorResponse(
        @JsonProperty("sector") String sector,
        @JsonProperty("basePrice") BigDecimal basePrice,
        @JsonProperty("max_capacity") Integer maxCapacity
    ) {
    }

    public record GarageSpotResponse(
        Long id,
        String sector,
        BigDecimal lat,
        BigDecimal lng
    ) {
    }
}
