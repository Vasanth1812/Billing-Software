package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDTO {
    private String id;
    private String branchName;
    private String branchCode;
    private String supermarket;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String branchManager;
    private String branchManagerId;
    private String contactNumber;
    private String warehouseLinked;
    private String warehouseId;
    private String status;
    private Integer vendorCount;
    private Integer totalStock;
    private Double monthlyRevenue;
    private LocalDateTime createdOn;
    private LocalDateTime updatedAt;
}
