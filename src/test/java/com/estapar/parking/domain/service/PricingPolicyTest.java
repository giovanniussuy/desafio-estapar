package com.estapar.parking.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PricingPolicyTest {

    private final PricingPolicy pricingPolicy = new PricingPolicy();

    @Test
    void shouldApplyDiscountWhenOccupancyBelowTwentyFivePercent() {
        BigDecimal multiplier = pricingPolicy.multiplierForOccupancy(new BigDecimal("0.24"));
        assertEquals(new BigDecimal("0.90"), multiplier);
    }

    @Test
    void shouldApplyNoAdjustmentWhenOccupancyBelowFiftyPercent() {
        BigDecimal multiplier = pricingPolicy.multiplierForOccupancy(new BigDecimal("0.30"));
        assertEquals(BigDecimal.ONE, multiplier);
    }

    @Test
    void shouldApplyTenPercentIncreaseWhenOccupancyBelowSeventyFivePercent() {
        BigDecimal multiplier = pricingPolicy.multiplierForOccupancy(new BigDecimal("0.70"));
        assertEquals(new BigDecimal("1.10"), multiplier);
    }

    @Test
    void shouldApplyTwentyFivePercentIncreaseWhenOccupancyAtOrAboveSeventyFivePercent() {
        BigDecimal multiplier = pricingPolicy.multiplierForOccupancy(new BigDecimal("0.75"));
        assertEquals(new BigDecimal("1.25"), multiplier);
    }

    @Test
    void shouldNotChargeWhenStayIsWithinThirtyMinutes() {
        Instant entry = Instant.parse("2025-01-01T12:00:00Z");
        Instant exit = Instant.parse("2025-01-01T12:30:00Z");

        BigDecimal amount = pricingPolicy.calculateExitAmount(entry, exit, new BigDecimal("10.00"));

        assertEquals(new BigDecimal("0.00"), amount);
    }

    @Test
    void shouldChargeCeilingHoursAfterGracePeriod() {
        Instant entry = Instant.parse("2025-01-01T12:00:00Z");
        Instant exit = Instant.parse("2025-01-01T13:31:00Z");

        BigDecimal amount = pricingPolicy.calculateExitAmount(entry, exit, new BigDecimal("10.00"));

        assertEquals(new BigDecimal("20.00"), amount);
    }

    @Test
    void shouldApplyMultiplierAndRoundToTwoDecimals() {
        BigDecimal adjusted = pricingPolicy.applyMultiplier(new BigDecimal("10.00"), new BigDecimal("1.155"));
        assertEquals(new BigDecimal("11.55"), adjusted);
    }

    @Test
    void shouldReturnZeroWhenExitIsBeforeEntry() {
        Instant entry = Instant.parse("2025-01-01T12:00:00Z");
        Instant exit = Instant.parse("2025-01-01T11:00:00Z");

        BigDecimal amount = pricingPolicy.calculateExitAmount(entry, exit, new BigDecimal("10.00"));

        assertEquals(new BigDecimal("0.00"), amount);
    }
}
