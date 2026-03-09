package com.estapar.parking.infrastructure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GarageConfigResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldDeserializeBasePriceAliasFromBasePrice() throws Exception {
        String json = """
            {
              \"garage\": [{\"sector\":\"A\",\"basePrice\": 12.5, \"max_capacity\": 5}],
              \"spots\": []
            }
            """;

        GarageConfigResponse response = mapper.readValue(json, GarageConfigResponse.class);

        assertEquals(new BigDecimal("12.5"), response.garage().get(0).basePrice());
    }
}
