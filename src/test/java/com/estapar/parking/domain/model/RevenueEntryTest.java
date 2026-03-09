package com.estapar.parking.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RevenueEntryTest {

    @Test
    void shouldReadAndWriteRevenueEntryFields() {
        RevenueEntry entry = new RevenueEntry();
        entry.setSectorCode("A");
        entry.setRevenueDate(LocalDate.of(2026, 3, 8));
        entry.setAmount(new BigDecimal("99.99"));
        Instant createdAt = Instant.parse("2026-03-09T01:00:00Z");
        entry.setCreatedAt(createdAt);

        assertEquals("A", entry.getSectorCode());
        assertEquals(LocalDate.of(2026, 3, 8), entry.getRevenueDate());
        assertEquals(new BigDecimal("99.99"), entry.getAmount());
        assertEquals(createdAt, entry.getCreatedAt());
        assertNull(entry.getId());
    }
}
