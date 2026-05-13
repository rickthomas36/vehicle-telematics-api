package com.ford.telematics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TelemetrySnapshot Entity
 *
 * This is the CORE object in any telematics system.
 * Every few seconds, a connected vehicle sends a packet of data — this is that packet.
 *
 * REAL WORLD CONTEXT:
 * Ford's vehicles send telemetry via cellular modem (embedded SIM) to
 * Ford's telematics servers. Engineers then query this data to:
 *   - Show vehicle status in the FordPass app
 *   - Diagnose faults remotely
 *   - Power predictive maintenance alerts
 *   - Train ADAS/autonomous driving models
 *
 * One vehicle might send thousands of these snapshots per day.
 * At Ford scale = billions of rows. This is why distributed databases matter.
 */
@Entity
@Table(name = "telemetry_snapshots",
       indexes = {
           // Index on VIN for fast lookups — critical at scale
           @Index(name = "idx_telemetry_vin", columnList = "vin"),
           // Index on timestamp for time-range queries
           @Index(name = "idx_telemetry_timestamp", columnList = "capturedAt")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetrySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /** Denormalized VIN for fast queries without joining to vehicles table */
    @Column(nullable = false, length = 17)
    private String vin;

    // ── LOCATION DATA ──────────────────────────────────────────────
    /**
     * GPS coordinates.
     * Real telematics uses GNSS (GPS + GLONASS + Galileo) for accuracy.
     * Precision matters: 6 decimal places = ~10cm accuracy.
     */
    private Double latitude;
    private Double longitude;
    private Double altitudeMeters;
    private Double headingDegrees;     // 0-360, where 0/360 = North

    // ── MOTION DATA ────────────────────────────────────────────────
    private Double speedKph;           // Speed in km/h
    private Double accelerationMps2;   // Acceleration m/s² (negative = braking)
    private Double odometryKm;         // Total distance this vehicle has ever traveled

    // ── POWERTRAIN DATA ────────────────────────────────────────────
    /**
     * Fuel level (ICE/Hybrid vehicles).
     * 0.0 = empty, 100.0 = full
     */
    private Double fuelLevelPercent;

    /**
     * Battery state of charge (EV/Hybrid vehicles).
     * This is what shows up in the FordPass app when you check
     * your F-150 Lightning battery level remotely.
     */
    private Double batteryStateOfChargePercent;
    private Double batteryVoltage;
    private Boolean isCharging;
    private Double estimatedRangeKm;

    // ── ENGINE / HEALTH DATA ───────────────────────────────────────
    private Double engineRpmValue;
    private Double engineCoolantTempCelsius;
    private Boolean checkEngineLightOn;
    private String activeDtcCodes;     // Diagnostic Trouble Codes, comma-separated

    // ── ENVIRONMENT ────────────────────────────────────────────────
    private Double ambientTempCelsius;
    private String weatherCondition;  // "clear", "rain", "snow", etc.

    // ── CONNECTIVITY ───────────────────────────────────────────────
    private Integer signalStrengthDbm;  // Cellular signal (-50 great, -110 poor)
    private String networkType;          // "4G", "5G"

    // ── TIMESTAMP ─────────────────────────────────────────────────
    /** When the vehicle captured this data (vehicle clock) */
    @Column(nullable = false)
    private LocalDateTime capturedAt;

    /** When our server received it (may differ due to connectivity gaps) */
    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onReceive() {
        receivedAt = LocalDateTime.now();
        if (capturedAt == null) capturedAt = receivedAt;
    }
}
