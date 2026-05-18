package com.Billing_System.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GSTReconciliationDTO {
    private String gstin;
    private String vendor;
    private String period;
    private boolean gstr1Filed;
    private BigDecimal portalAmount;
    private BigDecimal booksAmount;
    private BigDecimal itcHold;
    private String matchStatus;
    private String disputeNote;
    private boolean gstinMatch;
    private boolean notified;
    private boolean released;
    private boolean writtenOff;
}
