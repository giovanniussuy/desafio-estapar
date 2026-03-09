package com.estapar.parking.interfaces.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DtoRecordsTest {

    @Test
    void shouldExposeWebhookFields() {
        Instant entry = Instant.parse("2026-03-08T12:00:00Z");
        Instant exit = Instant.parse("2026-03-08T13:00:00Z");

        WebhookEventRequest request = new WebhookEventRequest(
            "ABC1234",
            entry,
            exit,
            10L,
            new BigDecimal("-23.561684"),
            new BigDecimal("-46.655981"),
            "A",
            "EXIT"
        );

        assertEquals("ABC1234", request.licensePlate());
        assertEquals(entry, request.entryTime());
        assertEquals(exit, request.exitTime());
        assertEquals(10L, request.spotId());
        assertEquals(new BigDecimal("-23.561684"), request.lat());
        assertEquals(new BigDecimal("-46.655981"), request.lng());
        assertEquals("A", request.sector());
        assertEquals("EXIT", request.eventType());
    }

    @Test
    void shouldExposeRevenueResponseAndQueryFields() {
        Instant now = Instant.now();
        RevenueResponse response = new RevenueResponse(new BigDecimal("10.00"), "BRL", now);
        RevenueQuery query = new RevenueQuery(LocalDate.of(2026, 3, 8), "A");

        assertEquals(new BigDecimal("10.00"), response.amount());
        assertEquals("BRL", response.currency());
        assertEquals(now, response.timestamp());
        assertEquals(LocalDate.of(2026, 3, 8), query.date());
        assertEquals("A", query.sector());
        assertNotNull(query);
    }
}
