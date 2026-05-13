package com.Billing_System.vendor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request body for adding a vendor bank account */
@Data
public class VendorBankAccountDTO {

    @NotBlank(message = "Account holder name is required")
    @Size(max = 100)
    private String accountHolderName;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100)
    private String bankName;

    @NotBlank(message = "Account number is required")
    @Size(min = 8, max = 18, message = "Account number must be 8–18 digits")
    @Pattern(regexp = "^[0-9]+$", message = "Account number must contain digits only")
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC format (e.g. HDFC0001234)")
    private String ifscCode;

    /** CURRENT | SAVINGS | CC | OD */
    @NotBlank(message = "Account type is required")
    private String accountType;

    private boolean isPrimary;
}
