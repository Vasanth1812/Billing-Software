package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "vendor_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "color", length = 20)
    private String color;
}
