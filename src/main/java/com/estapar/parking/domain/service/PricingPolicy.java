package com.estapar.parking.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class PricingPolicy {

    private static final BigDecimal OCCUPANCY_25 = new BigDecimal("0.25");
    private static final BigDecimal OCCUPANCY_50 = new BigDecimal("0.50");
    private static final BigDecimal OCCUPANCY_75 = new BigDecimal("0.75");

    public BigDecimal multiplierForOccupancy(BigDecimal occupancyRate) {
        if (occupancyRate.compareTo(OCCUPANCY_25) < 0) {
            return new BigDecimal("0.90");
        }
        if (occupancyRate.compareTo(OCCUPANCY_50) < 0) {
            return BigDecimal.ONE;
        }
        if (occupancyRate.compareTo(OCCUPANCY_75) < 0) {
            return new BigDecimal("1.10");
        }
        return new BigDecimal("1.25");
    }

    public BigDecimal applyMultiplier(BigDecimal basePrice, BigDecimal multiplier) {
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateExitAmount(Instant entryTime, Instant exitTime, BigDecimal hourlyPrice) {
        Duration duration = Duration.between(entryTime, exitTime);
        long parkedMinutes = Math.max(duration.toMinutes(), 0);
        if (parkedMinutes <= 30) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long chargedHours = (long) Math.ceil(parkedMinutes / 60.0d);
        return hourlyPrice.multiply(BigDecimal.valueOf(chargedHours)).setScale(2, RoundingMode.HALF_UP);
    }
}
