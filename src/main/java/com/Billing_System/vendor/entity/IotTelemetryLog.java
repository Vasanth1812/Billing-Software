package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "iot_telemetry_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IotTelemetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String vehicleReg;
    private String timestamp;
    private Double temperature;
    private Double humidity;
    private Boolean isBreach;
    private String status; // LOGGED, OVERRIDDEN, REJECTED_COMPLIANCE_BREACH
}
