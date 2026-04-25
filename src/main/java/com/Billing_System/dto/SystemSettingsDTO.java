package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingsDTO {
    private UUID id;
    private String businessName;
    private String gstin;
    private String address;
    private String phone;
    private String email;
    private String bankDetails;
    private String stateCode;
    private String state;
    private Boolean autoBackup;
    private String backupEmail;
}
