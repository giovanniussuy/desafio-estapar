package com.estapar.parking.domain.repository;

import com.estapar.parking.domain.model.ParkingSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    Optional<ParkingSession> findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc(String licensePlate);
}
