package com.estapar.parking.domain.repository;

import com.estapar.parking.domain.model.ParkingSpot;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ParkingSpot> findFirstBySectorCodeAndOccupiedFalseOrderByExternalIdAsc(String sectorCode);

    Optional<ParkingSpot> findFirstByExternalId(Long externalId);

    Optional<ParkingSpot> findFirstByLatBetweenAndLngBetween(
        BigDecimal minLat,
        BigDecimal maxLat,
        BigDecimal minLng,
        BigDecimal maxLng
    );

    Optional<ParkingSpot> findFirstBySectorCodeAndLatBetweenAndLngBetween(
        String sectorCode,
        BigDecimal minLat,
        BigDecimal maxLat,
        BigDecimal minLng,
        BigDecimal maxLng
    );
}
