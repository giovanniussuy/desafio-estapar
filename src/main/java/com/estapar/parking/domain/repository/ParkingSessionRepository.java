package com.estapar.parking.domain.repository;

import com.estapar.parking.domain.model.ParkingSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ParkingSession> findFirstByLicensePlateAndExitTimeIsNullOrderByEntryTimeDesc(String licensePlate);
}
