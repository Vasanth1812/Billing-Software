package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VendorDisputeResponseDTO {
    private UUID id;
    private UUID vendorId;
    private String vendorName;
    private String relatedInstrument;
    private String category;
    private String narrative;
    private String status;
    private LocalDateTime createdAt;
}
