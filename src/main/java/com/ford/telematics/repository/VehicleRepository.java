package com.ford.telematics.repository;

import com.ford.telematics.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * VehicleRepository
 *
 * Spring Data JPA auto-generates the SQL for all these methods.
 * You define the method name — Spring figures out the query.
 * This is a huge productivity boost over writing raw SQL for every operation.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /** Find a vehicle by VIN — the most common lookup in any telematics system */
    Optional<Vehicle> findByVin(String vin);

    /** Check if a VIN already exists before registering */
    boolean existsByVin(String vin);
}
