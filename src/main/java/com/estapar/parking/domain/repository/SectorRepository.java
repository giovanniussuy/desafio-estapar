package com.estapar.parking.domain.repository;

import com.estapar.parking.domain.model.Sector;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    Optional<Sector> findByCode(String code);

    List<Sector> findAllByOrderByCodeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sector s order by s.code asc")
    List<Sector> findAllByOrderByCodeAscForUpdate();
}
