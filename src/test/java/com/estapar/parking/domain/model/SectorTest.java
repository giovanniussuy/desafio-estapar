package com.estapar.parking.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SectorTest {

    @Test
    void shouldReportAvailabilityBasedOnCapacity() {
        Sector sector = new Sector();
        sector.setMaxCapacity(10);
        sector.setOccupiedSpots(9);

        assertTrue(sector.hasAvailableSpot());

        sector.setOccupiedSpots(10);
        assertFalse(sector.hasAvailableSpot());
    }

    @Test
    void shouldReturnOneWhenCapacityIsNullOrZero() {
        Sector nullCapacity = new Sector();
        nullCapacity.setOccupiedSpots(3);
        assertEquals(BigDecimal.ONE, nullCapacity.occupancyRate());

        Sector zeroCapacity = new Sector();
        zeroCapacity.setMaxCapacity(0);
        zeroCapacity.setOccupiedSpots(3);
        assertEquals(BigDecimal.ONE, zeroCapacity.occupancyRate());
    }

    @Test
    void shouldCalculateOccupancyRateWithScaleFour() {
        Sector sector = new Sector();
        sector.setMaxCapacity(3);
        sector.setOccupiedSpots(1);

        assertEquals(new BigDecimal("0.3333"), sector.occupancyRate());
    }
}
