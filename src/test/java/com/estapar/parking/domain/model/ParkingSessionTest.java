package com.estapar.parking.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ParkingSessionTest {

    @Test
    void shouldReadAndWriteSessionFieldsAndTrackOpenState() {
        Sector sector = new Sector();
        sector.setCode("A");

        ParkingSpot spot = new ParkingSpot();
        spot.setExternalId(7L);

        Instant entry = Instant.parse("2026-03-08T12:00:00Z");
        Instant parked = Instant.parse("2026-03-08T12:05:00Z");
        Instant exit = Instant.parse("2026-03-08T13:00:00Z");

        ParkingSession session = new ParkingSession();
        session.setLicensePlate("ABC1234");
        session.setSector(sector);
        session.setSpot(spot);
        session.setEntryTime(entry);
        session.setParkedTime(parked);
        session.setStatus(SessionStatus.PARKED);
        session.setPriceMultiplier(new BigDecimal("1.10"));
        session.setHourlyPriceApplied(new BigDecimal("12.50"));
        session.setAmountCharged(new BigDecimal("25.00"));

        assertEquals("ABC1234", session.getLicensePlate());
        assertEquals(sector, session.getSector());
        assertEquals(spot, session.getSpot());
        assertEquals(entry, session.getEntryTime());
        assertEquals(parked, session.getParkedTime());
        assertEquals(SessionStatus.PARKED, session.getStatus());
        assertEquals(new BigDecimal("1.10"), session.getPriceMultiplier());
        assertEquals(new BigDecimal("12.50"), session.getHourlyPriceApplied());
        assertEquals(new BigDecimal("25.00"), session.getAmountCharged());
        assertTrue(session.isOpen());

        session.setExitTime(exit);
        session.setStatus(SessionStatus.EXITED);
        assertEquals(exit, session.getExitTime());
        assertEquals(SessionStatus.EXITED, session.getStatus());
        assertFalse(session.isOpen());
        assertNull(session.getId());
    }
}
