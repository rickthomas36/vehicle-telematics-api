package com.ford.telematics;

import com.ford.telematics.dto.VehicleDtos.*;
import com.ford.telematics.model.TelemetrySnapshot;
import com.ford.telematics.model.Vehicle;
import com.ford.telematics.repository.TelemetryRepository;
import com.ford.telematics.repository.VehicleCommandRepository;
import com.ford.telematics.repository.VehicleRepository;
import com.ford.telematics.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VehicleServiceTest
 *
 * Unit tests using Mockito to mock dependencies.
 * We test the SERVICE logic in isolation — no real DB, no real HTTP.
 *
 * This is TDD (Test Driven Development) in practice:
 * Write tests → Run (fail) → Write code → Run (pass) → Refactor
 *
 * Ford's engineering teams require unit tests for all service logic.
 * Having tests in your project shows you understand professional development.
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private VehicleCommandRepository commandRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private static final String TEST_VIN = "1FTFW1ET5DFC10312";

    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        testVehicle = Vehicle.builder()
                .id(1L)
                .vin(TEST_VIN)
                .make("Ford")
                .model("F-150 Lightning")
                .year(2024)
                .color("Antimatter Blue")
                .vehicleType(Vehicle.VehicleType.EV)
                .registeredAt(LocalDateTime.now())
                .lastSeenAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new vehicle")
    void registerVehicle_success() {
        // Arrange — set up mock behavior
        when(vehicleRepository.existsByVin(TEST_VIN)).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(testVehicle);

        RegisterVehicleRequest request = RegisterVehicleRequest.builder()
                .vin(TEST_VIN)
                .make("Ford")
                .model("F-150 Lightning")
                .year(2024)
                .vehicleType(Vehicle.VehicleType.EV)
                .build();

        // Act — call the method we're testing
        VehicleResponse response = vehicleService.registerVehicle(request);

        // Assert — verify the result
        assertNotNull(response);
        assertEquals(TEST_VIN, response.getVin());
        assertEquals("Ford", response.getMake());
        assertEquals("F-150 Lightning", response.getModel());

        // Verify the repository was called exactly once
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Should throw exception when registering duplicate VIN")
    void registerVehicle_duplicateVin_throws() {
        // Arrange
        when(vehicleRepository.existsByVin(TEST_VIN)).thenReturn(true);

        RegisterVehicleRequest request = RegisterVehicleRequest.builder()
                .vin(TEST_VIN)
                .make("Ford")
                .model("Mustang Mach-E")
                .vehicleType(Vehicle.VehicleType.EV)
                .build();

        // Act & Assert — expect an exception
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> vehicleService.registerVehicle(request)
        );

        assertTrue(ex.getMessage().contains(TEST_VIN));

        // Verify we never tried to save
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return ONLINE status for recently seen vehicle")
    void getVehicleStatus_recentlySeen_returnsOnline() {
        // Arrange
        when(vehicleRepository.findByVin(TEST_VIN)).thenReturn(Optional.of(testVehicle));

        TelemetrySnapshot recentSnapshot = TelemetrySnapshot.builder()
                .vin(TEST_VIN)
                .vehicle(testVehicle)
                .batteryStateOfChargePercent(73.5)
                .speedKph(0.0)
                .latitude(42.3314)
                .longitude(-83.0458)
                .capturedAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(telemetryRepository.findFirstByVinOrderByCapturedAtDesc(TEST_VIN))
                .thenReturn(Optional.of(recentSnapshot));

        // Act
        VehicleStatusResponse status = vehicleService.getVehicleStatus(TEST_VIN);

        // Assert
        assertEquals("ONLINE", status.getConnectionStatus());
        assertEquals(73.5, status.getBatteryStateOfChargePercent());
    }

    @Test
    @DisplayName("Should return OFFLINE status for vehicle not seen in 30+ minutes")
    void getVehicleStatus_notSeenRecently_returnsOffline() {
        // Arrange — vehicle was last seen 2 hours ago
        testVehicle.setLastSeenAt(LocalDateTime.now().minusHours(2));
        when(vehicleRepository.findByVin(TEST_VIN)).thenReturn(Optional.of(testVehicle));

        TelemetrySnapshot oldSnapshot = TelemetrySnapshot.builder()
                .vin(TEST_VIN)
                .vehicle(testVehicle)
                .capturedAt(LocalDateTime.now().minusHours(2))
                .build();

        when(telemetryRepository.findFirstByVinOrderByCapturedAtDesc(TEST_VIN))
                .thenReturn(Optional.of(oldSnapshot));

        // Act
        VehicleStatusResponse status = vehicleService.getVehicleStatus(TEST_VIN);

        // Assert
        assertEquals("OFFLINE", status.getConnectionStatus());
    }

    @Test
    @DisplayName("Should throw exception when VIN not found")
    void getVehicle_notFound_throws() {
        when(vehicleRepository.findByVin("INVALIDVIN000000X")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> vehicleService.getVehicle("INVALIDVIN000000X"));
    }
}
