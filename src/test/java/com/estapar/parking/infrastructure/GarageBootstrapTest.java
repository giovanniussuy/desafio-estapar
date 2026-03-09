package com.estapar.parking.infrastructure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estapar.parking.domain.model.ParkingSpot;
import com.estapar.parking.domain.model.Sector;
import com.estapar.parking.domain.repository.ParkingSpotRepository;
import com.estapar.parking.domain.repository.SectorRepository;
import com.estapar.parking.infrastructure.client.GarageConfigResponse;
import com.estapar.parking.infrastructure.client.GarageConfigResponse.GarageSectorResponse;
import com.estapar.parking.infrastructure.client.GarageConfigResponse.GarageSpotResponse;
import com.estapar.parking.infrastructure.client.GarageSimulatorClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class GarageBootstrapTest {

    @Mock
    private GarageSimulatorClient simulatorClient;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Test
    void shouldSkipBootstrapWhenDatabaseAlreadyHasSectors() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, true);
        when(sectorRepository.count()).thenReturn(1L);

        assertDoesNotThrow(() -> bootstrap.run(null));

        verify(simulatorClient, never()).getGarageConfig();
        verify(sectorRepository, never()).save(any(Sector.class));
    }

    @Test
    void shouldThrowWhenSimulatorFailsAndBootstrapIsRequired() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, true);
        when(sectorRepository.count()).thenReturn(0L);
        when(simulatorClient.getGarageConfig()).thenThrow(new RestClientException("down"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bootstrap.run(null));

        assertEquals("Failed to load garage configuration from simulator", ex.getMessage());
    }

    @Test
    void shouldContinueWhenSimulatorFailsAndBootstrapIsOptional() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, false);
        when(sectorRepository.count()).thenReturn(0L);
        when(simulatorClient.getGarageConfig()).thenThrow(new RestClientException("down"));

        assertDoesNotThrow(() -> bootstrap.run(null));
        verify(sectorRepository, never()).save(any(Sector.class));
        verify(parkingSpotRepository, never()).save(any(ParkingSpot.class));
    }

    @Test
    void shouldThrowWhenPayloadIsInvalidAndBootstrapIsRequired() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, true);
        when(sectorRepository.count()).thenReturn(0L);
        when(simulatorClient.getGarageConfig()).thenReturn(new GarageConfigResponse(null, List.of()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bootstrap.run(null));

        assertEquals("Invalid garage configuration payload from simulator", ex.getMessage());
    }

    @Test
    void shouldContinueWhenPayloadIsInvalidAndBootstrapIsOptional() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, false);
        when(sectorRepository.count()).thenReturn(0L);
        when(simulatorClient.getGarageConfig()).thenReturn(new GarageConfigResponse(null, List.of()));

        assertDoesNotThrow(() -> bootstrap.run(null));
        verify(sectorRepository, never()).save(any(Sector.class));
        verify(parkingSpotRepository, never()).save(any(ParkingSpot.class));
    }

    @Test
    void shouldThrowWhenPayloadHasNullSpotsAndBootstrapIsRequired() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, true);
        when(sectorRepository.count()).thenReturn(0L);

        GarageSectorResponse sectorResponse = new GarageSectorResponse("A", new BigDecimal("10.00"), 2);
        when(simulatorClient.getGarageConfig()).thenReturn(new GarageConfigResponse(List.of(sectorResponse), null));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bootstrap.run(null));

        assertEquals("Invalid garage configuration payload from simulator", ex.getMessage());
    }

    @Test
    void shouldThrowWhenPayloadIsNullAndBootstrapIsRequired() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, true);
        when(sectorRepository.count()).thenReturn(0L);
        when(simulatorClient.getGarageConfig()).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bootstrap.run(null));

        assertEquals("Invalid garage configuration payload from simulator", ex.getMessage());
    }

    @Test
    void shouldIgnoreSpotWhenSectorDoesNotExistInGarageMap() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, true);
        when(sectorRepository.count()).thenReturn(0L);

        GarageSectorResponse sectorResponse = new GarageSectorResponse("A", new BigDecimal("10.00"), 2);
        GarageSpotResponse unknownSectorSpot = new GarageSpotResponse(2L, "B", new BigDecimal("-23.561685"), new BigDecimal("-46.655982"));
        when(simulatorClient.getGarageConfig()).thenReturn(new GarageConfigResponse(List.of(sectorResponse), List.of(unknownSectorSpot)));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> bootstrap.run(null));

        verify(parkingSpotRepository, never()).save(any(ParkingSpot.class));
    }

    @Test
    void shouldPersistSectorsAndSpotsWhenPayloadIsValid() {
        GarageBootstrap bootstrap = new GarageBootstrap(simulatorClient, sectorRepository, parkingSpotRepository, true);
        when(sectorRepository.count()).thenReturn(0L);

        GarageSectorResponse sectorResponse = new GarageSectorResponse("A", new BigDecimal("10.00"), 2);
        GarageSpotResponse spot1 = new GarageSpotResponse(1L, "A", new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
        GarageSpotResponse spot2 = new GarageSpotResponse(2L, "B", new BigDecimal("-23.561685"), new BigDecimal("-46.655982"));
        when(simulatorClient.getGarageConfig()).thenReturn(new GarageConfigResponse(List.of(sectorResponse), List.of(spot1, spot2)));

        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> bootstrap.run(null));

        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository).save(sectorCaptor.capture());
        Sector savedSector = sectorCaptor.getValue();
        assertEquals("A", savedSector.getCode());
        assertEquals(new BigDecimal("10.00"), savedSector.getBasePrice());
        assertEquals(2, savedSector.getMaxCapacity());
        assertEquals(0, savedSector.getOccupiedSpots());

        ArgumentCaptor<ParkingSpot> spotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(parkingSpotRepository).save(spotCaptor.capture());
        ParkingSpot savedSpot = spotCaptor.getValue();
        assertEquals(1L, savedSpot.getExternalId());
        assertEquals(false, savedSpot.isOccupied());
        assertEquals("A", savedSpot.getSector().getCode());
    }
}
