package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "warehouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "total_racks")
    private Integer totalRacks;

    @Column(name = "square_footage")
    private Double squareFootage;

    @Column(name = "temperature_controlled")
    private Boolean temperatureControlled;

    @Column(name = "manager_id", length = 50)
    private String managerId;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
