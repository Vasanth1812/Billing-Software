package com.Billing_System.vendor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request body for adding/updating a vendor location */
@Data
public class VendorLocationDTO {

    /** FACTORY | WAREHOUSE | OFFICE | SHIPPING_POINT */
    @NotBlank(message = "Location type is required")
    private String locationType;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 120)
    private String addressLine1;

    @Size(max = 120)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 60)
    private String city;

    @Size(min = 2, max = 2, message = "State code must be 2 characters (e.g. TN, MH, KA)")
    private String stateCode;

    @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must be 6 digits")
    private String pinCode;

    private boolean isPrimary;
}
