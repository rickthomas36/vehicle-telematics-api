package com.ford.telematics.service;

import com.ford.telematics.dto.VehicleDtos.*;
import com.ford.telematics.model.TelemetrySnapshot;
import com.ford.telematics.model.Vehicle;
import com.ford.telematics.model.VehicleCommand;
import com.ford.telematics.repository.TelemetryRepository;
import com.ford.telematics.repository.VehicleCommandRepository;
import com.ford.telematics.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VehicleService — Business Logic Layer
 *
 * The service layer is WHERE THE LOGIC LIVES.
 * Controllers handle HTTP. Repositories handle DB.
 * Services handle everything in between.
 *
 * This layered architecture (Controller → Service → Repository)
 * is the industry standard pattern at Ford, Amazon, and every
 * large Java shop. Learn this pattern deeply.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final TelemetryRepository telemetryRepository;
    private final VehicleCommandRepository commandRepository;

    // ─────────────────────────────────────────
    //  VEHICLE REGISTRATION
    // ─────────────────────────────────────────

    @Transactional
    public VehicleResponse registerVehicle(RegisterVehicleRequest request) {
        if (vehicleRepository.existsByVin(request.getVin())) {
            throw new IllegalArgumentException("Vehicle with VIN " + request.getVin() + " already registered");
        }

        Vehicle vehicle = Vehicle.builder()
                .vin(request.getVin())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
                .vehicleType(request.getVehicleType())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Registered new vehicle: {} {} {} (VIN: {})",
                saved.getYear(), saved.getMake(), saved.getModel(), saved.getVin());

        return toVehicleResponse(saved);
    }

    public VehicleResponse getVehicle(String vin) {
        Vehicle vehicle = findVehicleOrThrow(vin);
        return toVehicleResponse(vehicle);
    }

    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::toVehicleResponse)
                .toList();
    }

    // ─────────────────────────────────────────
    //  TELEMETRY INGESTION
    // ─────────────────────────────────────────

    /**
     * Ingest a telemetry snapshot from a vehicle.
     *
     * In a real Ford system, this endpoint is called by the vehicle's
     * embedded telematics control unit (TCU) every few seconds via
     * cellular data. This is the "heartbeat" of connected vehicles.
     */
    @Transactional
    public void ingestTelemetry(String vin, TelemetryIngestRequest request) {
        Vehicle vehicle = findVehicleOrThrow(vin);

        TelemetrySnapshot snapshot = TelemetrySnapshot.builder()
                .vehicle(vehicle)
                .vin(vin)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .altitudeMeters(request.getAltitudeMeters())
                .headingDegrees(request.getHeadingDegrees())
                .speedKph(request.getSpeedKph())
                .accelerationMps2(request.getAccelerationMps2())
                .odometryKm(request.getOdometryKm())
                .fuelLevelPercent(request.getFuelLevelPercent())
                .batteryStateOfChargePercent(request.getBatteryStateOfChargePercent())
                .batteryVoltage(request.getBatteryVoltage())
                .isCharging(request.getIsCharging())
                .estimatedRangeKm(request.getEstimatedRangeKm())
                .engineRpmValue(request.getEngineRpmValue())
                .engineCoolantTempCelsius(request.getEngineCoolantTempCelsius())
                .checkEngineLightOn(request.getCheckEngineLightOn())
                .activeDtcCodes(request.getActiveDtcCodes())
                .ambientTempCelsius(request.getAmbientTempCelsius())
                .weatherCondition(request.getWeatherCondition())
                .signalStrengthDbm(request.getSignalStrengthDbm())
                .networkType(request.getNetworkType())
                .capturedAt(request.getCapturedAt() != null
                        ? request.getCapturedAt()
                        : LocalDateTime.now())
                .build();

        telemetryRepository.save(snapshot);

        // Update vehicle's last seen timestamp
        vehicle.setLastSeenAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);

        log.debug("Ingested telemetry for VIN {} — speed: {} kph, battery: {}%",
                vin, request.getSpeedKph(), request.getBatteryStateOfChargePercent());
    }

    // ─────────────────────────────────────────
    //  VEHICLE STATUS (Latest Telemetry)
    // ─────────────────────────────────────────

    /**
     * Get the current status of a vehicle.
     * This is exactly what the FordPass app calls when you open it —
     * it fetches the most recent telemetry snapshot for your VIN.
     */
    public VehicleStatusResponse getVehicleStatus(String vin) {
        Vehicle vehicle = findVehicleOrThrow(vin);

        TelemetrySnapshot latest = telemetryRepository
                .findFirstByVinOrderByCapturedAtDesc(vin)
                .orElse(null);

        String connectionStatus = determineConnectionStatus(vehicle.getLastSeenAt());

        if (latest == null) {
            return VehicleStatusResponse.builder()
                    .vin(vin)
                    .make(vehicle.getMake())
                    .model(vehicle.getModel())
                    .connectionStatus("NEVER_CONNECTED")
                    .build();
        }

        return VehicleStatusResponse.builder()
                .vin(vin)
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .latitude(latest.getLatitude())
                .longitude(latest.getLongitude())
                .speedKph(latest.getSpeedKph())
                .fuelLevelPercent(latest.getFuelLevelPercent())
                .batteryStateOfChargePercent(latest.getBatteryStateOfChargePercent())
                .isCharging(latest.getIsCharging())
                .estimatedRangeKm(latest.getEstimatedRangeKm())
                .checkEngineLightOn(latest.getCheckEngineLightOn())
                .activeDtcCodes(latest.getActiveDtcCodes())
                .lastUpdated(latest.getCapturedAt())
                .connectionStatus(connectionStatus)
                .build();
    }

    // ─────────────────────────────────────────
    //  REMOTE COMMANDS
    // ─────────────────────────────────────────

    /**
     * Issue a remote command to a vehicle.
     * Think: FordPass "Remote Lock" button → this method → DB → vehicle polls → executes.
     */
    @Transactional
    public VehicleCommand issueCommand(String vin, CommandRequest request) {
        findVehicleOrThrow(vin); // Validate VIN exists

        VehicleCommand command = VehicleCommand.builder()
                .vin(vin)
                .commandType(request.getCommandType())
                .status(VehicleCommand.CommandStatus.PENDING)
                .issuedBy(request.getIssuedBy() != null ? request.getIssuedBy() : "API_USER")
                .build();

        VehicleCommand saved = commandRepository.save(command);
        log.info("Issued command {} to VIN {} — command ID: {}", request.getCommandType(), vin, saved.getId());
        return saved;
    }

    public List<VehicleCommand> getCommandHistory(String vin) {
        findVehicleOrThrow(vin);
        return commandRepository.findByVinOrderByIssuedAtDesc(vin);
    }

    // ─────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────

    private Vehicle findVehicleOrThrow(String vin) {
        return vehicleRepository.findByVin(vin)
                .orElseThrow(() -> new IllegalArgumentException("No vehicle found with VIN: " + vin));
    }

    private String determineConnectionStatus(LocalDateTime lastSeen) {
        if (lastSeen == null) return "OFFLINE";
        long minutesAgo = java.time.Duration.between(lastSeen, LocalDateTime.now()).toMinutes();
        if (minutesAgo < 5)  return "ONLINE";
        if (minutesAgo < 30) return "DELAYED";
        return "OFFLINE";
    }

    private VehicleResponse toVehicleResponse(Vehicle v) {
        return VehicleResponse.builder()
                .vin(v.getVin())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .color(v.getColor())
                .vehicleType(v.getVehicleType())
                .registeredAt(v.getRegisteredAt())
                .lastSeenAt(v.getLastSeenAt())
                .build();
    }
}
