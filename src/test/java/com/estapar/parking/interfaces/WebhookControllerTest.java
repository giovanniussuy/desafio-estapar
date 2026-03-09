package com.estapar.parking.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.estapar.parking.domain.service.ParkingEventService;
import com.estapar.parking.interfaces.error.DomainException;
import com.estapar.parking.interfaces.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebhookController.class)
@Import(GlobalExceptionHandler.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParkingEventService parkingEventService;

    @Test
    void shouldReturnOkForValidWebhookPayload() throws Exception {
        String payload = """
            {
              \"license_plate\": \"ABC1234\",
              \"entry_time\": \"2026-03-08T12:00:00Z\",
              \"event_type\": \"ENTRY\"
            }
            """;

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verify(parkingEventService).processEvent(any());
    }

    @Test
    void shouldReturnBadRequestWhenPayloadValidationFails() throws Exception {
        String payload = """
            {
              \"license_plate\": \"\",
              \"event_type\": \"ENTRY\"
            }
            """;

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid request payload"));
    }

    @Test
    void shouldReturnBadRequestWhenDomainExceptionIsThrown() throws Exception {
        String payload = """
            {
              \"license_plate\": \"ABC1234\",
              \"entry_time\": \"2026-03-08T12:00:00Z\",
              \"event_type\": \"ENTRY\"
            }
            """;

        doThrow(new DomainException("Vehicle already inside garage"))
            .when(parkingEventService).processEvent(any());

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Vehicle already inside garage"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedExceptionOccurs() throws Exception {
        String payload = """
            {
              \"license_plate\": \"ABC1234\",
              \"entry_time\": \"2026-03-08T12:00:00Z\",
              \"event_type\": \"ENTRY\"
            }
            """;

        doThrow(new RuntimeException("boom"))
            .when(parkingEventService).processEvent(any());

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
