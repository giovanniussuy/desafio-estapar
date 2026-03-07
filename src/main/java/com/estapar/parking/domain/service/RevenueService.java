package com.estapar.parking.domain.service;

import com.estapar.parking.domain.repository.RevenueEntryRepository;
import com.estapar.parking.interfaces.dto.RevenueResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class RevenueService {

    private final RevenueEntryRepository revenueEntryRepository;

    public RevenueService(RevenueEntryRepository revenueEntryRepository) {
        this.revenueEntryRepository = revenueEntryRepository;
    }

    public RevenueResponse getRevenue(LocalDate date, String sector) {
        BigDecimal amount = revenueEntryRepository.sumByDateAndSector(date, sector.toUpperCase()).setScale(2, RoundingMode.HALF_UP);
        return new RevenueResponse(amount, "BRL", Instant.now());
    }
}
