package com.estapar.parking.interfaces;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.estapar.parking.domain.service.RevenueService;
import com.estapar.parking.interfaces.controller.RevenueController;
import com.estapar.parking.interfaces.dto.RevenueResponse;
import com.estapar.parking.interfaces.error.DomainException;
import com.estapar.parking.interfaces.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RevenueController.class)
@Import(GlobalExceptionHandler.class)
class RevenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RevenueService revenueService;

    @Test
    void shouldReturnRevenueForValidQuery() throws Exception {
        RevenueResponse response = new RevenueResponse(new BigDecimal("25.50"), "BRL", Instant.parse("2026-03-08T12:00:00Z"));
        when(revenueService.getRevenue(LocalDate.of(2026, 3, 8), "A")).thenReturn(response);

        mockMvc.perform(get("/revenue")
                .queryParam("date", "2026-03-08")
                .queryParam("sector", "A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(25.50))
            .andExpect(jsonPath("$.currency").value("BRL"));

        verify(revenueService).getRevenue(eq(LocalDate.of(2026, 3, 8)), eq("A"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenDateIsMissing() throws Exception {
        mockMvc.perform(get("/revenue").queryParam("sector", "A"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Unexpected server error"))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void shouldReturnBadRequestWhenDomainExceptionIsThrown() throws Exception {
        doThrow(new DomainException("invalid sector"))
            .when(revenueService).getRevenue(LocalDate.of(2026, 3, 8), "A");

        mockMvc.perform(get("/revenue")
                .queryParam("date", "2026-03-08")
                .queryParam("sector", "A"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invalid sector"))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(header().exists("X-Trace-Id"));
    }
}
