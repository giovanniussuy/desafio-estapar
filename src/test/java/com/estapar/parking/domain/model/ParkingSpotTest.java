package com.estapar.parking.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ParkingSpotTest {

    @Test
    void shouldReadAndWriteAllFields() {
        Sector sector = new Sector();
        sector.setCode("A");

        ParkingSpot spot = new ParkingSpot();
        spot.setExternalId(11L);
        spot.setSector(sector);
        spot.setLat(new BigDecimal("-23.56168400"));
        spot.setLng(new BigDecimal("-46.65598100"));
        spot.setOccupied(true);

        assertEquals(11L, spot.getExternalId());
        assertEquals(sector, spot.getSector());
        assertEquals(new BigDecimal("-23.56168400"), spot.getLat());
        assertEquals(new BigDecimal("-46.65598100"), spot.getLng());
        assertTrue(spot.isOccupied());

        spot.setOccupied(false);
        assertFalse(spot.isOccupied());
        assertNull(spot.getId());
    }
}
