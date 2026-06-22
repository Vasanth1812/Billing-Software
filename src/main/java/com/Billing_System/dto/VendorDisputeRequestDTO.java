package com.Billing_System.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorDisputeRequestDTO {
    @NotBlank(message = "Related instrument is required")
    private String relatedInstrument;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Narrative is required")
    private String narrative;
}
