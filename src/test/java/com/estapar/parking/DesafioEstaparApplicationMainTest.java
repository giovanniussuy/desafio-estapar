package com.estapar.parking;

import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class DesafioEstaparApplicationMainTest {

    @Test
    void shouldInvokeSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplicationMock = Mockito.mockStatic(SpringApplication.class)) {
            String[] args = new String[] {"--test"};
            DesafioEstaparApplication.main(args);

            springApplicationMock.verify(
                () -> SpringApplication.run(DesafioEstaparApplication.class, args),
                times(1)
            );
        }
    }
}
