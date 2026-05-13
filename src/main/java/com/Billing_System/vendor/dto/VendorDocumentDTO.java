package com.Billing_System.vendor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request body for uploading/adding a vendor document */
@Data
public class VendorDocumentDTO {

    /**
     * GSTIN | FSSAI | PAN | TRADE_LICENSE | DRUG_LICENSE | CIN | OTHER
     */
    @NotBlank(message = "Document type is required")
    private String docType;

    @NotBlank(message = "Document number is required")
    @Size(max = 50)
    private String docNumber;

    /**
     * Expiry date in yyyy-MM-dd format.
     * Leave blank for non-expiring documents (e.g. PAN).
     */
    private String expiryDate;

    /**
     * File reference — path or URL after the file is uploaded.
     * Frontend uploads the file first, then sends the reference here.
     */
    @Size(max = 500)
    private String fileReference;
}
