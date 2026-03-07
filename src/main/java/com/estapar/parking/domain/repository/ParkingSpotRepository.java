package com.estapar.parking.domain.repository;

import com.estapar.parking.domain.model.ParkingSpot;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    Optional<ParkingSpot> findFirstBySectorCodeAndOccupiedFalseOrderByExternalIdAsc(String sectorCode);

    Optional<ParkingSpot> findFirstByLatBetweenAndLngBetween(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng);
}
