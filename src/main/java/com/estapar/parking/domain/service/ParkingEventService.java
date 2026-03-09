package com.estapar.parking.domain.service;

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
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingEventService {

    private static final BigDecimal COORDINATE_DELTA = new BigDecimal("0.000001");

    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final RevenueEntryRepository revenueEntryRepository;
    private final PricingPolicy pricingPolicy;
    private final ZoneId revenueTimeZone;

    public ParkingEventService(
        SectorRepository sectorRepository,
        ParkingSpotRepository parkingSpotRepository,
        ParkingSessionRepository parkingSessionRepository,
        RevenueEntryRepository revenueEntryRepository,
        PricingPolicy pricingPolicy,
        @Value("${app.revenue.time-zone:America/Sao_Paulo}") String revenueTimeZone
    ) {
        this.sectorRepository = sectorRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.revenueEntryRepository = revenueEntryRepository;
        this.pricingPolicy = pricingPolicy;
        this.revenueTimeZone = ZoneId.of(revenueTimeZone);
    }

    @Transactional
    public void processEvent(WebhookEventRequest request) {
        String eventType = request.eventType().trim().toUpperCase();
        switch (eventType) {
            case "ENTRY" -> handleEntry(request);
            case "PARKED" -> handleParked(request);
            case "EXIT" -> handleExit(request);
            default -> throw new DomainException("Unsupported event_type: " + request.eventType());
        }
    }

    private void handleEntry(WebhookEventRequest request) {
        if (request.entryTime() == null) {
            throw new DomainException("entry_time is required for ENTRY event");
        }

        parkingSessionRepository.findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc(request.licensePlate())
            .ifPresent(active -> {
                throw new DomainException("Vehicle already inside garage");
            });

        Sector selectedSector = selectSectorWithAvailability();
        ParkingSpot reservedSpot = parkingSpotRepository
            .findFirstBySectorCodeAndOccupiedFalseOrderByExternalIdAsc(selectedSector.getCode())
            .orElseThrow(() -> new DomainException("No available spot for sector " + selectedSector.getCode()));

        BigDecimal multiplier = pricingPolicy.multiplierForOccupancy(selectedSector.occupancyRate());
        BigDecimal hourlyPrice = pricingPolicy.applyMultiplier(selectedSector.getBasePrice(), multiplier);

        ParkingSession session = new ParkingSession();
        session.setLicensePlate(request.licensePlate());
        session.setSector(selectedSector);
        session.setSpot(reservedSpot);
        session.setEntryTime(request.entryTime());
        session.setStatus(SessionStatus.ENTERED);
        session.setPriceMultiplier(multiplier);
        session.setHourlyPriceApplied(hourlyPrice);

        reservedSpot.setOccupied(true);
        selectedSector.setOccupiedSpots(selectedSector.getOccupiedSpots() + 1);

        parkingSpotRepository.save(reservedSpot);
        sectorRepository.save(selectedSector);
        parkingSessionRepository.save(session);
    }

    private void handleParked(WebhookEventRequest request) {
        ParkingSpot matchedSpot = findParkedSpot(request);

        ParkingSession session = parkingSessionRepository
            .findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc(request.licensePlate())
            .orElseThrow(() -> new DomainException("Open session not found for vehicle"));

        ParkingSpot currentSpot = resolveSessionSpot(session);
        boolean changingSpot = currentSpot == null || !matchedSpot.getId().equals(currentSpot.getId());

        if (changingSpot && matchedSpot.isOccupied()) {
            throw new DomainException("Target spot already occupied");
        }

        if (changingSpot && currentSpot != null) {
            currentSpot.setOccupied(false);
            parkingSpotRepository.save(currentSpot);
        }

        if (!matchedSpot.isOccupied()) {
            matchedSpot.setOccupied(true);
            parkingSpotRepository.save(matchedSpot);
        }

        session.setSpot(matchedSpot);

        session.setStatus(SessionStatus.PARKED);
        session.setParkedTime(Instant.now());
        parkingSessionRepository.save(session);
    }

    private ParkingSpot findParkedSpot(WebhookEventRequest request) {
        if (request.spotId() != null) {
            ParkingSpot spot = parkingSpotRepository.findFirstByExternalId(request.spotId())
                .orElseThrow(() -> new DomainException("spot_id not found"));

            if (request.sector() != null && !request.sector().isBlank()) {
                String informedSector = request.sector().trim().toUpperCase();
                String spotSector = spot.getSector().getCode().toUpperCase();
                if (!spotSector.equals(informedSector)) {
                    throw new DomainException("spot_id does not belong to informed sector");
                }
            }
            return spot;
        }

        if (request.lat() == null || request.lng() == null) {
            throw new DomainException("spot_id or lat/lng are required for PARKED event");
        }
        ParkingSpot matchedSpot;
        if (request.sector() == null || request.sector().isBlank()) {
            matchedSpot = findSpotByCoordinates(request.lat(), request.lng());
        } else {
            String parkedSector = request.sector().trim().toUpperCase();
            matchedSpot = findSpotByCoordinates(parkedSector, request.lat(), request.lng());
        }
        if (matchedSpot == null) {
            throw new DomainException("Target spot not found");
        }
        return matchedSpot;
    }

    private void handleExit(WebhookEventRequest request) {
        if (request.exitTime() == null) {
            throw new DomainException("exit_time is required for EXIT event");
        }

        ParkingSession session = parkingSessionRepository
            .findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc(request.licensePlate())
            .orElseThrow(() -> new DomainException("Open session not found for vehicle"));

        Instant exitTime = request.exitTime();
        BigDecimal amount = pricingPolicy.calculateExitAmount(session.getEntryTime(), exitTime, session.getHourlyPriceApplied());

        ParkingSpot currentSpot = resolveSessionSpot(session);
        if (currentSpot != null) {
            currentSpot.setOccupied(false);
            parkingSpotRepository.save(currentSpot);
            session.setSpot(currentSpot);
        }

        Sector sector = session.getSector();
        sector.setOccupiedSpots(Math.max(0, sector.getOccupiedSpots() - 1));
        sectorRepository.save(sector);

        session.setStatus(SessionStatus.EXITED);
        session.setExitTime(exitTime);
        session.setAmountCharged(amount);
        parkingSessionRepository.save(session);

        RevenueEntry revenueEntry = new RevenueEntry();
        revenueEntry.setSectorCode(sector.getCode());
        revenueEntry.setRevenueDate(LocalDate.ofInstant(exitTime, revenueTimeZone));
        revenueEntry.setAmount(amount);
        revenueEntry.setCreatedAt(Instant.now());
        revenueEntryRepository.save(revenueEntry);
    }

    private Sector selectSectorWithAvailability() {
        List<Sector> sectors = sectorRepository.findAllByOrderByCodeAscForUpdate();
        return sectors.stream()
            .filter(Sector::hasAvailableSpot)
            .findFirst()
            .orElseThrow(() -> new DomainException("Garage is full"));
    }

    private ParkingSpot findSpotByCoordinates(String sectorCode, BigDecimal lat, BigDecimal lng) {
        BigDecimal minLat = lat.subtract(COORDINATE_DELTA);
        BigDecimal maxLat = lat.add(COORDINATE_DELTA);
        BigDecimal minLng = lng.subtract(COORDINATE_DELTA);
        BigDecimal maxLng = lng.add(COORDINATE_DELTA);

        return parkingSpotRepository.findFirstBySectorCodeAndLatBetweenAndLngBetween(
                sectorCode,
                minLat,
                maxLat,
                minLng,
                maxLng
            )
            .orElse(null);
    }

    private ParkingSpot findSpotByCoordinates(BigDecimal lat, BigDecimal lng) {
        BigDecimal minLat = lat.subtract(COORDINATE_DELTA);
        BigDecimal maxLat = lat.add(COORDINATE_DELTA);
        BigDecimal minLng = lng.subtract(COORDINATE_DELTA);
        BigDecimal maxLng = lng.add(COORDINATE_DELTA);

        return parkingSpotRepository.findFirstByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng)
            .orElse(null);
    }

    private ParkingSpot resolveSessionSpot(ParkingSession session) {
        if (session.getSpot() == null || session.getSpot().getId() == null) {
            return null;
        }
        return parkingSpotRepository.findById(session.getSpot().getId()).orElse(session.getSpot());
    }
}
