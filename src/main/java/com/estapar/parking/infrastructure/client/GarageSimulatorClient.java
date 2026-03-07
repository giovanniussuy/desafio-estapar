package com.estapar.parking.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GarageSimulatorClient {

    private final RestClient restClient;

    public GarageSimulatorClient(@Value("${simulator.base-url:http://localhost:3000}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public GarageConfigResponse getGarageConfig() {
        return restClient.get()
            .uri("/garage")
            .retrieve()
            .body(GarageConfigResponse.class);
    }
}
