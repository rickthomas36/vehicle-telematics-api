package com.ford.telematics.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vehicle Entity
 *
 * Represents a connected vehicle registered in the telematics system.
 *
 * WHAT IS A VIN?
 * Vehicle Identification Number — a 17-character unique ID stamped on every car.
 * Example: 1FTFW1ET5DFC10312 (a real Ford F-150 VIN format)
 * Ford's telematics APIs use VIN as the primary key for all vehicle operations.
 * This is exactly how Ford's connected vehicle platform identifies cars.
 */
@Entity
@Table(name = "vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * VIN — the unique identifier for every vehicle.
     * Real Ford APIs always route requests by VIN, e.g.:
     * GET /vehicles/{vin}/status
     * POST /vehicles/{vin}/commands/lock
     */
    @NotBlank
    @Pattern(regexp = "[A-HJ-NPR-Z0-9]{17}",
             message = "VIN must be 17 alphanumeric characters (no I, O, Q)")
    @Column(unique = true, nullable = false, length = 17)
    private String vin;

    @NotBlank
    @Column(nullable = false)
    private String make;          // e.g. "Ford"

    @NotBlank
    @Column(nullable = false)
    private String model;         // e.g. "F-150"

    private Integer year;

    private String color;

    /**
     * Vehicle type affects what telemetry we collect.
     * ICE = Internal Combustion Engine (fuel level matters)
     * EV  = Electric Vehicle (battery % matters)
     * HYBRID = both
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    private LocalDateTime lastSeenAt;

    /**
     * Relationship: one vehicle has many telemetry snapshots over time.
     * Each snapshot = one data point sent from the vehicle.
     */
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TelemetrySnapshot> telemetryHistory;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        lastSeenAt = LocalDateTime.now();
    }

    public enum VehicleType {
        ICE,        // Gas powered
        EV,         // Electric (F-150 Lightning, Mustang Mach-E)
        HYBRID      // Both
    }
}
