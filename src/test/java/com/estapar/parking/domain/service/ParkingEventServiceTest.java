package com.estapar.parking.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estapar.parking.domain.model.ParkingSession;
import com.estapar.parking.domain.model.ParkingSpot;
import com.estapar.parking.domain.model.RevenueEntry;
import com.estapar.parking.domain.model.Sector;
import com.estapar.parking.domain.model.SessionStatus;
import com.estapar.parking.domain.repository.ParkingSessionRepository;
import com.estapar.parking.domain.repository.ParkingSpotRepository;
import com.estapar.parking.domain.repository.RevenueEntryRepository;
import com.estapar.parking.domain.repository.SectorRepository;
import com.estapar.parking.interfaces.dto.WebhookEventRequest;
import com.estapar.parking.interfaces.error.DomainException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingEventServiceTest {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Mock
    private ParkingSessionRepository parkingSessionRepository;

    @Mock
    private RevenueEntryRepository revenueEntryRepository;

    @Mock
    private PricingPolicy pricingPolicy;

    private ParkingEventService service;

    @BeforeEach
    void setUp() {
        service = new ParkingEventService(
            sectorRepository,
            parkingSpotRepository,
            parkingSessionRepository,
            revenueEntryRepository,
            pricingPolicy,
            "America/Sao_Paulo"
        );
    }

    @Test
    void shouldProcessEntryAndReserveFirstAvailableSpot() {
        WebhookEventRequest request = new WebhookEventRequest(
            "ABC1234",
            Instant.parse("2026-03-08T12:00:00Z"),
            null,
            null,
            null,
            null,
            null,
            "ENTRY"
        );

        Sector sector = sector("A", 10, 3, new BigDecimal("10.00"));
        ParkingSpot spot = spot(1L, sector, false, new BigDecimal("-23.56"), new BigDecimal("-46.65"));

        when(parkingSessionRepository.findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc("ABC1234"))
            .thenReturn(Optional.empty());
        when(sectorRepository.findAllByOrderByCodeAscForUpdate()).thenReturn(List.of(sector));
        when(parkingSpotRepository.findFirstBySectorCodeAndOccupiedFalseOrderByExternalIdAsc("A"))
            .thenReturn(Optional.of(spot));
        when(pricingPolicy.multiplierForOccupancy(new BigDecimal("0.3000"))).thenReturn(new BigDecimal("1.10"));
        when(pricingPolicy.applyMultiplier(new BigDecimal("10.00"), new BigDecimal("1.10"))).thenReturn(new BigDecimal("11.00"));

        service.processEvent(request);

        ArgumentCaptor<ParkingSession> sessionCaptor = ArgumentCaptor.forClass(ParkingSession.class);
        verify(parkingSessionRepository).save(sessionCaptor.capture());
        ParkingSession savedSession = sessionCaptor.getValue();

        assertEquals("ABC1234", savedSession.getLicensePlate());
        assertEquals(SessionStatus.ENTERED, savedSession.getStatus());
        assertEquals(new BigDecimal("1.10"), savedSession.getPriceMultiplier());
        assertEquals(new BigDecimal("11.00"), savedSession.getHourlyPriceApplied());
        assertEquals(sector, savedSession.getSector());
        assertEquals(spot, savedSession.getSpot());

        assertEquals(4, sector.getOccupiedSpots());
        assertEquals(true, spot.isOccupied());
        verify(sectorRepository).save(sector);
        verify(parkingSpotRepository).save(spot);
    }

    @Test
    void shouldProcessParkedAndMoveVehicleToMatchedSpotByCoordinates() {
        Sector sector = sector("A", 10, 1, new BigDecimal("10.00"));
        ParkingSpot previousSpot = spot(10L, sector, true, new BigDecimal("-23.500000"), new BigDecimal("-46.500000"));
        ParkingSpot matchedSpot = spot(11L, sector, false, new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
        setId(previousSpot, 100L);
        setId(matchedSpot, 200L);

        ParkingSession session = new ParkingSession();
        session.setSpot(previousSpot);

        WebhookEventRequest request = new WebhookEventRequest(
            "ABC1234",
            null,
            null,
            null,
            new BigDecimal("-23.561684"),
            new BigDecimal("-46.655981"),
            "A",
            "PARKED"
        );

        when(parkingSessionRepository.findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc("ABC1234"))
            .thenReturn(Optional.of(session));
        when(parkingSpotRepository.findById(100L)).thenReturn(Optional.of(previousSpot));
        when(parkingSpotRepository.findFirstBySectorCodeAndLatBetweenAndLngBetween(any(), any(), any(), any(), any()))
            .thenReturn(Optional.of(matchedSpot));

        service.processEvent(request);

        assertEquals(SessionStatus.PARKED, session.getStatus());
        assertEquals(matchedSpot, session.getSpot());
        assertNotNull(session.getParkedTime());
        verify(parkingSpotRepository).save(previousSpot);
        verify(parkingSpotRepository).save(matchedSpot);
        verify(parkingSessionRepository).save(session);
    }

    @Test
    void shouldProcessParkedAndMoveVehicleToMatchedSpotBySpotId() {
        Sector sector = sector("A", 10, 1, new BigDecimal("10.00"));
        ParkingSpot previousSpot = spot(10L, sector, true, new BigDecimal("-23.500000"), new BigDecimal("-46.500000"));
        ParkingSpot matchedSpot = spot(11L, sector, false, new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
        setId(previousSpot, 100L);
        setId(matchedSpot, 200L);

        ParkingSession session = new ParkingSession();
        session.setSpot(previousSpot);

        WebhookEventRequest request = new WebhookEventRequest(
            "ABC1234",
            null,
            null,
            11L,
            null,
            null,
            "A",
            "PARKED"
        );

        when(parkingSessionRepository.findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc("ABC1234"))
            .thenReturn(Optional.of(session));
        when(parkingSpotRepository.findById(100L)).thenReturn(Optional.of(previousSpot));
        when(parkingSpotRepository.findFirstByExternalId(11L)).thenReturn(Optional.of(matchedSpot));

        service.processEvent(request);

        assertEquals(SessionStatus.PARKED, session.getStatus());
        assertEquals(matchedSpot, session.getSpot());
        assertNotNull(session.getParkedTime());
        verify(parkingSpotRepository).save(previousSpot);
        verify(parkingSpotRepository).save(matchedSpot);
        verify(parkingSessionRepository).save(session);
        verify(parkingSpotRepository, never()).findFirstBySectorCodeAndLatBetweenAndLngBetween(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectParkedWhenSpotIdAndCoordinatesAreMissing() {
        WebhookEventRequest request = new WebhookEventRequest("ABC1234", null, null, null, null, null, null, "PARKED");

        DomainException ex = assertThrows(DomainException.class, () -> service.processEvent(request));

        assertEquals("spot_id or lat/lng are required for PARKED event", ex.getMessage());
    }

    @Test
    void shouldProcessParkedByCoordinatesWhenSectorIsMissing() {
        Sector sector = sector("A", 10, 1, new BigDecimal("10.00"));
        ParkingSpot previousSpot = spot(10L, sector, true, new BigDecimal("-23.500000"), new BigDecimal("-46.500000"));
        ParkingSpot matchedSpot = spot(11L, sector, false, new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
        setId(previousSpot, 100L);
        setId(matchedSpot, 200L);

        ParkingSession session = new ParkingSession();
        session.setSpot(previousSpot);

        WebhookEventRequest request = new WebhookEventRequest(
            "ABC1234",
            null,
            null,
            null,
            new BigDecimal("-23.561684"),
            new BigDecimal("-46.655981"),
            null,
            "PARKED"
        );

        when(parkingSessionRepository.findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc("ABC1234"))
            .thenReturn(Optional.of(session));
        when(parkingSpotRepository.findById(100L)).thenReturn(Optional.of(previousSpot));
        when(parkingSpotRepository.findFirstByLatBetweenAndLngBetween(any(), any(), any(), any()))
            .thenReturn(Optional.of(matchedSpot));

        service.processEvent(request);

        assertEquals(SessionStatus.PARKED, session.getStatus());
        assertEquals(matchedSpot, session.getSpot());
        assertNotNull(session.getParkedTime());
        verify(parkingSpotRepository).save(previousSpot);
        verify(parkingSpotRepository).save(matchedSpot);
        verify(parkingSessionRepository).save(session);
    }

    @Test
    void shouldRejectParkedWhenSpotIdSectorDoesNotMatchInformedSector() {
        Sector spotSector = sector("B", 10, 0, new BigDecimal("10.00"));
        ParkingSpot matchedSpot = spot(11L, spotSector, false, new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));

        WebhookEventRequest request = new WebhookEventRequest(
            "ABC1234",
            null,
            null,
            11L,
            null,
            null,
            "A",
            "PARKED"
        );

        when(parkingSpotRepository.findFirstByExternalId(11L)).thenReturn(Optional.of(matchedSpot));

        DomainException ex = assertThrows(DomainException.class, () -> service.processEvent(request));

        assertEquals("spot_id does not belong to informed sector", ex.getMessage());
    }

    @Test
    void shouldRejectParkedWhenTargetSpotIsAlreadyOccupied() {
        Sector sector = sector("A", 10, 1, new BigDecimal("10.00"));
        ParkingSpot previousSpot = spot(10L, sector, true, new BigDecimal("-23.500000"), new BigDecimal("-46.500000"));
        ParkingSpot matchedSpot = spot(11L, sector, true, new BigDecimal("-23.561684"), new BigDecimal("-46.655981"));
        setId(previousSpot, 100L);
        setId(matchedSpot, 200L);

        ParkingSession session = new ParkingSession();
        session.setSpot(previousSpot);

        WebhookEventRequest request = new WebhookEventRequest(
            "ABC1234",
            null,
            null,
            11L,
            null,
            null,
            "A",
            "PARKED"
        );

        when(parkingSessionRepository.findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc("ABC1234"))
            .thenReturn(Optional.of(session));
        when(parkingSpotRepository.findById(100L)).thenReturn(Optional.of(previousSpot));
        when(parkingSpotRepository.findFirstByExternalId(11L)).thenReturn(Optional.of(matchedSpot));

        DomainException ex = assertThrows(DomainException.class, () -> service.processEvent(request));

        assertEquals("Target spot already occupied", ex.getMessage());
    }

    @Test
    void shouldProcessExitAndCreateRevenueEntryUsingConfiguredTimeZone() {
        Instant entry = Instant.parse("2026-03-08T22:00:00Z");
        Instant exit = Instant.parse("2026-03-09T00:30:00Z");

        Sector sector = sector("A", 10, 5, new BigDecimal("10.00"));
        ParkingSpot spot = spot(10L, sector, true, new BigDecimal("-23.56"), new BigDecimal("-46.65"));
        setId(spot, 300L);

        ParkingSession session = new ParkingSession();
        session.setSector(sector);
        session.setSpot(spot);
        session.setEntryTime(entry);
        session.setHourlyPriceApplied(new BigDecimal("10.00"));

        WebhookEventRequest request = new WebhookEventRequest("ABC1234", null, exit, null, null, null, null, "EXIT");

        when(parkingSessionRepository.findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc("ABC1234"))
            .thenReturn(Optional.of(session));
        when(parkingSpotRepository.findById(300L)).thenReturn(Optional.of(spot));
        when(pricingPolicy.calculateExitAmount(entry, exit, new BigDecimal("10.00")))
            .thenReturn(new BigDecimal("30.00"));

        service.processEvent(request);

        assertEquals(SessionStatus.EXITED, session.getStatus());
        assertEquals(exit, session.getExitTime());
        assertEquals(new BigDecimal("30.00"), session.getAmountCharged());
        assertEquals(false, spot.isOccupied());
        assertEquals(4, sector.getOccupiedSpots());

        ArgumentCaptor<RevenueEntry> revenueCaptor = ArgumentCaptor.forClass(RevenueEntry.class);
        verify(revenueEntryRepository).save(revenueCaptor.capture());
        RevenueEntry savedRevenue = revenueCaptor.getValue();
        assertEquals("A", savedRevenue.getSectorCode());
        assertEquals(LocalDate.of(2026, 3, 8), savedRevenue.getRevenueDate());
        assertEquals(new BigDecimal("30.00"), savedRevenue.getAmount());
        assertNotNull(savedRevenue.getCreatedAt());
    }

    @Test
    void shouldRejectExitWhenNoOpenSessionExists() {
        WebhookEventRequest request = new WebhookEventRequest("ABC1234", null, Instant.now(), null, null, null, null, "EXIT");

        when(parkingSessionRepository.findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc(anyString()))
            .thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class, () -> service.processEvent(request));

        assertEquals("Open session not found for vehicle", ex.getMessage());
    }

    private static Sector sector(String code, int maxCapacity, int occupied, BigDecimal basePrice) {
        Sector sector = new Sector();
        sector.setCode(code);
        sector.setMaxCapacity(maxCapacity);
        sector.setOccupiedSpots(occupied);
        sector.setBasePrice(basePrice);
        return sector;
    }

    private static ParkingSpot spot(Long externalId, Sector sector, boolean occupied, BigDecimal lat, BigDecimal lng) {
        ParkingSpot spot = new ParkingSpot();
        spot.setExternalId(externalId);
        spot.setSector(sector);
        spot.setOccupied(occupied);
        spot.setLat(lat);
        spot.setLng(lng);
        return spot;
    }

    private static void setId(ParkingSpot spot, Long id) {
        try {
            java.lang.reflect.Field field = ParkingSpot.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(spot, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
