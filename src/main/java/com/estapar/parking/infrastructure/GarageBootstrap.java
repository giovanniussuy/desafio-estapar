package com.estapar.parking.infrastructure;

import com.estapar.parking.domain.model.ParkingSpot;
import com.estapar.parking.domain.model.Sector;
import com.estapar.parking.domain.repository.ParkingSpotRepository;
import com.estapar.parking.domain.repository.SectorRepository;
import com.estapar.parking.infrastructure.client.GarageConfigResponse;
import com.estapar.parking.infrastructure.client.GarageSimulatorClient;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Component
public class GarageBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(GarageBootstrap.class);

    private final GarageSimulatorClient simulatorClient;
    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final boolean bootstrapRequired;

    public GarageBootstrap(
        GarageSimulatorClient simulatorClient,
        SectorRepository sectorRepository,
        ParkingSpotRepository parkingSpotRepository,
        @Value("${simulator.bootstrap.required:true}") boolean bootstrapRequired
    ) {
        this.simulatorClient = simulatorClient;
        this.sectorRepository = sectorRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.bootstrapRequired = bootstrapRequired;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (sectorRepository.count() > 0) {
            return;
        }

        GarageConfigResponse config;
        try {
            config = simulatorClient.getGarageConfig();
        } catch (RestClientException e) {
            if (bootstrapRequired) {
                throw new IllegalStateException("Failed to load garage configuration from simulator", e);
            }
            logger.warn("Failed to load garage configuration from simulator. Proceeding without initial data load because simulator.bootstrap.required=false.", e);
            return;
        }

        if (config == null || config.garage() == null || config.spots() == null) {
            if (bootstrapRequired) {
                throw new IllegalStateException("Invalid garage configuration payload from simulator");
            }
            logger.warn("Invalid garage configuration payload. Proceeding without initial data load because simulator.bootstrap.required=false.");
            return;
        }

        Map<String, Sector> sectorMap = config.garage().stream()
            .map(sectorResponse -> {
                Sector sector = new Sector();
                sector.setCode(sectorResponse.sector());
                sector.setBasePrice(sectorResponse.basePrice());
                sector.setMaxCapacity(sectorResponse.maxCapacity());
                sector.setOccupiedSpots(0);
                return sectorRepository.save(sector);
            })
            .collect(java.util.stream.Collectors.toMap(Sector::getCode, Function.identity()));

        config.spots().forEach(spotResponse -> {
            Sector sector = sectorMap.get(spotResponse.sector());
            if (sector == null) {
                return;
            }
            ParkingSpot spot = new ParkingSpot();
            spot.setExternalId(spotResponse.id());
            spot.setSector(sector);
            spot.setLat(spotResponse.lat());
            spot.setLng(spotResponse.lng());
            spot.setOccupied(false);
            parkingSpotRepository.save(spot);
        });
    }
}
