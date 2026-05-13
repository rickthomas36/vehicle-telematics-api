package com.ford.telematics.dto;

import com.ford.telematics.model.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTOs — Data Transfer Objects
 *
 * WHY USE DTOs INSTEAD OF EXPOSING ENTITIES DIRECTLY?
 * 1. Security: don't expose internal database IDs or sensitive fields
 * 2. Flexibility: API shape can change independently of DB schema
 * 3. Validation: annotate what the CLIENT must provide vs what we generate
 *
 * This is standard practice at Ford and every large tech company.
 */
public class VehicleDtos {

    // ── REQUEST: Register a new vehicle ──────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterVehicleRequest {

        @NotBlank(message = "VIN is required")
        @Pattern(regexp = "[A-HJ-NPR-Z0-9]{17}",
                 message = "VIN must be 17 alphanumeric characters")
        private String vin;

        @NotBlank(message = "Make is required")
        private String make;

        @NotBlank(message = "Model is required")
        private String model;

        private Integer year;
        private String color;

        @NotNull(message = "Vehicle type is required (ICE, EV, or HYBRID)")
        private Vehicle.VehicleType vehicleType;
    }

    // ── RESPONSE: Vehicle summary ─────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleResponse {
        private String vin;
        private String make;
        private String model;
        private Integer year;
        private String color;
        private Vehicle.VehicleType vehicleType;
        private LocalDateTime registeredAt;
        private LocalDateTime lastSeenAt;
    }

    // ── REQUEST: Ingest telemetry data from a vehicle ─────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TelemetryIngestRequest {
        // Location
        private Double latitude;
        private Double longitude;
        private Double altitudeMeters;
        private Double headingDegrees;

        // Motion
        private Double speedKph;
        private Double accelerationMps2;
        private Double odometryKm;

        // Powertrain
        private Double fuelLevelPercent;
        private Double batteryStateOfChargePercent;
        private Double batteryVoltage;
        private Boolean isCharging;
        private Double estimatedRangeKm;

        // Engine health
        private Double engineRpmValue;
        private Double engineCoolantTempCelsius;
        private Boolean checkEngineLightOn;
        private String activeDtcCodes;

        // Environment
        private Double ambientTempCelsius;
        private String weatherCondition;

        // Connectivity
        private Integer signalStrengthDbm;
        private String networkType;

        // When vehicle captured this (vehicle timestamp)
        private LocalDateTime capturedAt;
    }

    // ── RESPONSE: Current vehicle status (what FordPass shows you) ──
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleStatusResponse {
        private String vin;
        private String make;
        private String model;

        // Latest telemetry values
        private Double latitude;
        private Double longitude;
        private Double speedKph;
        private Double fuelLevelPercent;
        private Double batteryStateOfChargePercent;
        private Boolean isCharging;
        private Double estimatedRangeKm;
        private Boolean checkEngineLightOn;
        private String activeDtcCodes;
        private LocalDateTime lastUpdated;

        // Derived status
        private String connectionStatus;  // "ONLINE", "OFFLINE", "DELAYED"
    }

    // ── REQUEST: Issue a remote command ───────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandRequest {
        @NotNull(message = "Command type is required")
        private com.ford.telematics.model.VehicleCommand.CommandType commandType;
        private String issuedBy;
    }
}
