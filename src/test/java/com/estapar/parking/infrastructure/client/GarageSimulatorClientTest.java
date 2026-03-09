package com.estapar.parking.infrastructure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GarageSimulatorClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldFetchGarageConfigurationFromSimulator() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/garage", exchange -> {
            String body = """
                {
                  \"garage\": [
                    {\"sector\":\"A\",\"base_price\":10.00,\"max_capacity\":5}
                  ],
                  \"spots\": [
                    {\"id\":1,\"sector\":\"A\",\"lat\":-23.561684,\"lng\":-46.655981}
                  ]
                }
                """;
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        GarageSimulatorClient client = new GarageSimulatorClient(baseUrl);

        GarageConfigResponse response = client.getGarageConfig();

        assertEquals(1, response.garage().size());
        assertEquals("A", response.garage().get(0).sector());
        assertEquals(1, response.spots().size());
        assertEquals(1L, response.spots().get(0).id());
    }
}
