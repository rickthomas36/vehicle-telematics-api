package com.ford.telematics.repository;

import com.ford.telematics.model.TelemetrySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetrySnapshot, Long> {

    /** Get the most recent snapshot for a vehicle — used for "current status" */
    Optional<TelemetrySnapshot> findFirstByVinOrderByCapturedAtDesc(String vin);

    /** Get all snapshots for a vehicle in a time range — used for trip history */
    List<TelemetrySnapshot> findByVinAndCapturedAtBetweenOrderByCapturedAtAsc(
        String vin, LocalDateTime from, LocalDateTime to);

    /** Count how many data points we have for a vehicle */
    long countByVin(String vin);

    /**
     * Custom JPQL query — find snapshots where check engine light is on.
     * Ford's maintenance alert system uses queries like this to proactively
     * notify customers before they even notice a problem.
     */
    @Query("SELECT t FROM TelemetrySnapshot t WHERE t.vin = :vin " +
           "AND t.checkEngineLightOn = true " +
           "ORDER BY t.capturedAt DESC")
    List<TelemetrySnapshot> findAlertSnapshots(@Param("vin") String vin);

    /**
     * Find vehicles with low battery — powers "low charge" push notifications
     * in the FordPass app for F-150 Lightning / Mustang Mach-E owners.
     */
    @Query("SELECT DISTINCT t.vin FROM TelemetrySnapshot t " +
           "WHERE t.batteryStateOfChargePercent < :threshold " +
           "AND t.capturedAt > :since")
    List<String> findVinsWithLowBattery(
        @Param("threshold") double threshold,
        @Param("since") LocalDateTime since);
}
