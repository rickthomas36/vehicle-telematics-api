package com.ford.telematics.repository;

import com.ford.telematics.model.VehicleCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleCommandRepository extends JpaRepository<VehicleCommand, Long> {

    List<VehicleCommand> findByVinOrderByIssuedAtDesc(String vin);

    List<VehicleCommand> findByVinAndStatus(String vin, VehicleCommand.CommandStatus status);
}
