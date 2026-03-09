package com.estapar.parking.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estapar.parking.domain.repository.RevenueEntryRepository;
import com.estapar.parking.interfaces.dto.RevenueResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock
    private RevenueEntryRepository revenueEntryRepository;

    @InjectMocks
    private RevenueService revenueService;

    @Test
    void shouldUppercaseSectorAndRoundAmount() {
        LocalDate date = LocalDate.of(2026, 3, 8);
        when(revenueEntryRepository.sumByDateAndSector(date, "A")).thenReturn(new BigDecimal("10.456"));

        RevenueResponse response = revenueService.getRevenue(date, "a");

        verify(revenueEntryRepository).sumByDateAndSector(date, "A");
        assertEquals(new BigDecimal("10.46"), response.amount());
        assertEquals("BRL", response.currency());
    }
}
