package com.estapar.parking.interfaces.controller;

import com.estapar.parking.domain.service.RevenueService;
import com.estapar.parking.interfaces.dto.RevenueResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class RevenueController {

    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping("/revenue")
    public RevenueResponse getRevenue(
        @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam @NotBlank String sector
    ) {
        return revenueService.getRevenue(date, sector);
    }
}
