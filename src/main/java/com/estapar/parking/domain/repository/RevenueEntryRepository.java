package com.estapar.parking.domain.repository;

import com.estapar.parking.domain.model.RevenueEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevenueEntryRepository extends JpaRepository<RevenueEntry, Long> {

    @Query("select coalesce(sum(r.amount), 0) from RevenueEntry r where r.revenueDate = :date and r.sectorCode = :sector")
    BigDecimal sumByDateAndSector(@Param("date") LocalDate date, @Param("sector") String sector);
}
