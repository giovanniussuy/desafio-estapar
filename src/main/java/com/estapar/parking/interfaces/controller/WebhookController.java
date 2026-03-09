package com.estapar.parking.interfaces.controller;

import com.estapar.parking.domain.service.ParkingEventService;
import com.estapar.parking.interfaces.dto.WebhookEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final ParkingEventService parkingEventService;

    public WebhookController(ParkingEventService parkingEventService) {
        this.parkingEventService = parkingEventService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveEvent(@Valid @RequestBody WebhookEventRequest request) {
        parkingEventService.processEvent(request);
        return ResponseEntity.ok().build();
    }
}
