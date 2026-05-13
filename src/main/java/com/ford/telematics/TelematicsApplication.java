package com.ford.telematics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vehicle Telematics API
 *
 * Simulates a Ford-style connected vehicle backend service.
 * Vehicles submit telemetry data (speed, location, battery, fuel)
 * and the API stores, retrieves, and allows remote commands.
 *
 * Learning goal: understand how real automotive APIs work —
 * the same pattern Ford uses in their connected vehicle platform.
 */
@SpringBootApplication
public class TelematicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelematicsApplication.class, args);
        System.out.println("\n🚗  Vehicle Telematics API is running!");
        System.out.println("📊  H2 Console: http://localhost:8080/h2-console");
        System.out.println("🔗  API Base:   http://localhost:8080/api/v1\n");
    }
}
