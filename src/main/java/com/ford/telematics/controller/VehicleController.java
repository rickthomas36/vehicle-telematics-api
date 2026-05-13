package com.ford.telematics.controller;

import com.ford.telematics.dto.VehicleDtos.*;
import com.ford.telematics.model.VehicleCommand;
import com.ford.telematics.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * VehicleController — REST API Layer
 *
 * This is the HTTP front door of the application.
 * It receives requests, delegates to the service, and returns responses.
 * Controllers should be THIN — no business logic here.
 *
 * API DESIGN FOLLOWS FORD'S PATTERN:
 * All vehicle operations are scoped to a VIN:
 *   /api/v1/vehicles/{vin}/status
 *   /api/v1/vehicles/{vin}/telemetry
 *   /api/v1/vehicles/{vin}/commands
 *
 * This is RESTful resource-based design — the VIN IS the resource.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    // ─────────────────────────────────────────
    //  VEHICLE REGISTRATION
    // ─────────────────────────────────────────

    /**
     * POST /api/v1/vehicles
     * Register a new connected vehicle in the telematics system.
     *
     * Example request body:
     * {
     *   "vin": "1FTFW1ET5DFC10312",
     *   "make": "Ford",
     *   "model": "F-150 Lightning",
     *   "year": 2024,
     *   "color": "Antimatter Blue",
     *   "vehicleType": "EV"
     * }
     */
    @PostMapping
    public ResponseEntity<VehicleResponse> registerVehicle(
            @Valid @RequestBody RegisterVehicleRequest request) {
        log.info("Registering vehicle with VIN: {}", request.getVin());
        VehicleResponse response = vehicleService.registerVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/vehicles
     * List all registered vehicles.
     */
    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    /**
     * GET /api/v1/vehicles/{vin}
     * Get vehicle registration details by VIN.
     */
    @GetMapping("/{vin}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable String vin) {
        return ResponseEntity.ok(vehicleService.getVehicle(vin.toUpperCase()));
    }

    // ─────────────────────────────────────────
    //  VEHICLE STATUS (Latest Telemetry)
    // ─────────────────────────────────────────

    /**
     * GET /api/v1/vehicles/{vin}/status
     * Get the current status of a vehicle — most recent telemetry.
     *
     * This is the endpoint the FordPass app calls when you open it.
     * It returns battery level, location, doors locked, etc.
     */
    @GetMapping("/{vin}/status")
    public ResponseEntity<VehicleStatusResponse> getVehicleStatus(@PathVariable String vin) {
        return ResponseEntity.ok(vehicleService.getVehicleStatus(vin.toUpperCase()));
    }

    // ─────────────────────────────────────────
    //  TELEMETRY INGESTION
    // ─────────────────────────────────────────

    /**
     * POST /api/v1/vehicles/{vin}/telemetry
     * Ingest a telemetry data packet from a vehicle.
     *
     * In production, this is called by the vehicle's TCU (Telematics Control Unit)
     * automatically, not by users. Think of it as the vehicle "phoning home."
     *
     * Example: An F-150 Lightning driving down I-75 sends this every 5 seconds:
     * {
     *   "latitude": 42.3314,
     *   "longitude": -83.0458,
     *   "speedKph": 105.2,
     *   "batteryStateOfChargePercent": 73.5,
     *   "estimatedRangeKm": 210.0,
     *   "isCharging": false,
     *   "capturedAt": "2024-01-15T14:30:00"
     * }
     */
    @PostMapping("/{vin}/telemetry")
    public ResponseEntity<Map<String, String>> ingestTelemetry(
            @PathVariable String vin,
            @RequestBody TelemetryIngestRequest request) {
        vehicleService.ingestTelemetry(vin.toUpperCase(), request);
        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "vin", vin.toUpperCase(),
                "message", "Telemetry snapshot recorded"
        ));
    }

    // ─────────────────────────────────────────
    //  REMOTE COMMANDS
    // ─────────────────────────────────────────

    /**
     * POST /api/v1/vehicles/{vin}/commands
     * Issue a remote command to a vehicle.
     *
     * Example: Lock a vehicle remotely (FordPass "Lock" button)
     * {
     *   "commandType": "LOCK",
     *   "issuedBy": "user@example.com"
     * }
     *
     * Available commands: LOCK, UNLOCK, REMOTE_START, REMOTE_STOP,
     *                     PANIC_ALARM, CHARGE_START, CHARGE_STOP,
     *                     CLIMATE_PRECONDITION, LOCATE
     */
    @PostMapping("/{vin}/commands")
    public ResponseEntity<VehicleCommand> issueCommand(
            @PathVariable String vin,
            @Valid @RequestBody CommandRequest request) {
        VehicleCommand command = vehicleService.issueCommand(vin.toUpperCase(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(command);
    }

    /**
     * GET /api/v1/vehicles/{vin}/commands
     * Get command history for a vehicle.
     */
    @GetMapping("/{vin}/commands")
    public ResponseEntity<List<VehicleCommand>> getCommandHistory(@PathVariable String vin) {
        return ResponseEntity.ok(vehicleService.getCommandHistory(vin.toUpperCase()));
    }

    // ─────────────────────────────────────────
    //  HEALTH CHECK
    // ─────────────────────────────────────────

    /**
     * GET /api/v1/vehicles/health
     * Simple health check — used by load balancers and monitoring tools.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Vehicle Telematics API",
                "version", "1.0.0"
        ));
    }
}
