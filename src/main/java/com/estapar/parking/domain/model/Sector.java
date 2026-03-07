package com.estapar.parking.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "sectors")
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private Integer occupiedSpots = 0;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getOccupiedSpots() {
        return occupiedSpots;
    }

    public void setOccupiedSpots(Integer occupiedSpots) {
        this.occupiedSpots = occupiedSpots;
    }

    public boolean hasAvailableSpot() {
        return occupiedSpots < maxCapacity;
    }

    public BigDecimal occupancyRate() {
        if (maxCapacity == null || maxCapacity == 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(occupiedSpots)
            .divide(BigDecimal.valueOf(maxCapacity), 4, java.math.RoundingMode.HALF_UP);
    }
}
