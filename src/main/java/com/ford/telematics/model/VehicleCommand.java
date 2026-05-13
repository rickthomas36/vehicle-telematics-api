package com.ford.telematics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VehicleCommand Entity
 *
 * Represents a remote command sent TO a vehicle.
 *
 * REAL WORLD CONTEXT:
 * This is how FordPass works when you tap "Remote Start" on your phone.
 * Your phone → Ford API server → command queued → vehicle modem polls for commands
 * → vehicle executes → vehicle confirms back to server → app shows success.
 *
 * This is called a "command-and-control" pattern in automotive telematics.
 * The vehicle doesn't receive commands instantly — it polls periodically.
 * This is why remote start sometimes takes 10-30 seconds.
 */
@Entity
@Table(name = "vehicle_commands")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 17)
    private String vin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandType commandType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommandStatus status;

    /** Who issued this command (user ID or system) */
    private String issuedBy;

    /** When the command was created */
    @Column(nullable = false)
    private LocalDateTime issuedAt;

    /** When the vehicle acknowledged/executed it */
    private LocalDateTime executedAt;

    /** Any response payload from the vehicle */
    private String responsePayload;

    @PrePersist
    protected void onCreate() {
        issuedAt = LocalDateTime.now();
        if (status == null) status = CommandStatus.PENDING;
    }

    /**
     * Command types — mirrors real FordPass remote features.
     * Ford's actual API supports all of these.
     */
    public enum CommandType {
        LOCK,               // Lock doors
        UNLOCK,             // Unlock doors
        REMOTE_START,       // Start engine/climate system
        REMOTE_STOP,        // Stop remote start
        PANIC_ALARM,        // Sound horn + flash lights
        CHARGE_START,       // Start charging (EV)
        CHARGE_STOP,        // Stop charging (EV)
        CLIMATE_PRECONDITION, // Heat/cool cabin before entering
        LOCATE              // Ping vehicle for current location
    }

    /**
     * Command lifecycle:
     * PENDING → SENT → ACKNOWLEDGED → EXECUTED
     *                              → FAILED
     *        → EXPIRED (vehicle didn't respond in time)
     */
    public enum CommandStatus {
        PENDING,        // Created, not yet sent to vehicle
        SENT,           // Delivered to vehicle modem
        ACKNOWLEDGED,   // Vehicle received it
        EXECUTED,       // Vehicle completed the action
        FAILED,         // Something went wrong on vehicle
        EXPIRED         // Vehicle never responded (offline?)
    }
}
