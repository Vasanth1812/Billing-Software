package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "dock_appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DockAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String store;
    private String vendorName;
    private String date;
    private String slot;
    private String dockType;
    private String vehicleType;
    private String vehicleReg;
    private String driverName;
    private String driverMobile;
    private Integer estLoad;
    private String poNumbers;
    private String specialReq;
    private String status; // PENDING, CHECKED_IN, GATE_DENY, COMPLETED
}
