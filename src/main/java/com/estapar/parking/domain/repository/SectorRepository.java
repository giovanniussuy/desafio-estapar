package com.estapar.parking.domain.repository;

import com.estapar.parking.domain.model.Sector;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    Optional<Sector> findByCode(String code);

    List<Sector> findAllByOrderByCodeAsc();
}
