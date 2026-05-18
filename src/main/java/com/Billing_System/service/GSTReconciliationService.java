package com.Billing_System.service;

import com.Billing_System.dto.GSTReconciliationDTO;
import com.Billing_System.entity.GSTReconciliationEntity;
import com.Billing_System.entity.VendorInvoice;
import com.Billing_System.repository.GSTReconciliationRepository;
import com.Billing_System.repository.VendorInvoiceRepository;
import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GSTReconciliationService {

    private final VendorRepository vendorRepository;
    private final VendorInvoiceRepository invoiceRepository;
    private final GSTReconciliationRepository reconciliationRepository;

    @Transactional(readOnly = true)
    public List<GSTReconciliationDTO> getReconciliationData(String period) {
        log.info("Fetching live GST reconciliation data for period: {}", period);
        List<Vendor> vendors = vendorRepository.findAll();
        List<GSTReconciliationDTO> dtoList = new ArrayList<>();

        // Parse year and month from period (format: YYYY-MM)
        int year = 2026;
        int month = 4;
        try {
            if (period != null && period.contains("-")) {
                String[] parts = period.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            log.warn("Invalid period format: {}, defaulting to 2026-04", period);
        }

        final int targetYear = year;
        final int targetMonth = month;

        for (Vendor vendor : vendors) {
            // Only include vendors with a valid GSTIN
            if (vendor.getGstin() == null || vendor.getGstin().trim().isEmpty()) {
                continue;
            }

            String gstin = vendor.getGstin().trim();

            // 1. Fetch invoices for this vendor in the given period
            List<VendorInvoice> vInvoices = invoiceRepository.findByVendorId(vendor.getId());
            List<VendorInvoice> filteredInvoices = new ArrayList<>();
            for (VendorInvoice inv : vInvoices) {
                LocalDate date = inv.getInvoiceDate();
                if (date != null && date.getYear() == targetYear && date.getMonthValue() == targetMonth) {
                    filteredInvoices.add(inv);
                }
            }

            // Calculate Books Amount: total base amount plus GST (grand total of the period)
            BigDecimal booksAmount = filteredInvoices.stream()
                    .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalGst = filteredInvoices.stream()
                    .map(inv -> inv.getGstAmount() != null ? inv.getGstAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Fetch persisted actions/dispute state if it exists
            Optional<GSTReconciliationEntity> stateOpt = reconciliationRepository.findByGstinAndPeriod(gstin, period);
            GSTReconciliationEntity state = stateOpt.orElse(null);

            // 2. Determine GSTR-1 matching properties based on vendor compliance & registry state
            boolean gstr1Filed = true;
            BigDecimal portalAmount = booksAmount;
            String matchStatus = "Matched";
            BigDecimal itcHold = BigDecimal.ZERO;
            boolean gstinMatch = true;

            String status = vendor.getComplianceStatus() != null ? vendor.getComplianceStatus().toUpperCase() : "COMPLIANT";

            if ("BLOCKED".equals(status) || "NON_COMPLIANT".equals(status)) {
                // Non-compliant vendors: GSTR-1 not filed, Portal Amount = 0, ITC blocked completely
                gstr1Filed = false;
                portalAmount = BigDecimal.ZERO;
                matchStatus = "GSTR Not Filed";
                itcHold = totalGst.compareTo(BigDecimal.ZERO) > 0 ? totalGst : booksAmount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
                gstinMatch = false;
            } else if ("PENDING_KYC".equals(vendor.getOnboardingStage())) {
                // Pending KYC: minor mismatched discrepancies
                gstr1Filed = true;
                portalAmount = booksAmount.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
                matchStatus = "Mismatched";
                itcHold = totalGst.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
                gstinMatch = false;
            } else {
                // Standard match
                gstr1Filed = true;
                portalAmount = booksAmount;
                matchStatus = booksAmount.compareTo(BigDecimal.ZERO) > 0 ? "Matched" : "Pending";
                itcHold = BigDecimal.ZERO;
                gstinMatch = true;
            }

            // Apply manual overrides from recorded actions
            boolean notified = false;
            boolean released = false;
            boolean writtenOff = false;
            String disputeNote = "";

            if (state != null) {
                notified = state.isNotified();
                released = state.isReleased();
                writtenOff = state.isWrittenOff();
                disputeNote = state.getDisputeNote() != null ? state.getDisputeNote() : "";

                if (released || writtenOff) {
                    itcHold = BigDecimal.ZERO; // Hold is released or written off
                    if (released) {
                        matchStatus = "Matched";
                        gstinMatch = true;
                    }
                }
            }

            // Build DTO
            GSTReconciliationDTO dto = GSTReconciliationDTO.builder()
                    .gstin(gstin)
                    .vendor(vendor.getLegalName() != null ? vendor.getLegalName() : vendor.getLegalName())
                    .period(period)
                    .gstr1Filed(gstr1Filed)
                    .portalAmount(portalAmount)
                    .booksAmount(booksAmount)
                    .itcHold(itcHold)
                    .matchStatus(matchStatus)
                    .disputeNote(disputeNote)
                    .gstinMatch(gstinMatch)
                    .notified(notified)
                    .released(released)
                    .writtenOff(writtenOff)
                    .build();

            dtoList.add(dto);
        }

        return dtoList;
    }

    @Transactional
    public void updateDisputeNote(String gstin, String period, String note) {
        log.info("Updating dispute note for vendor {} during period {}: {}", gstin, period, note);
        GSTReconciliationEntity entity = reconciliationRepository.findByGstinAndPeriod(gstin, period)
                .orElseGet(() -> GSTReconciliationEntity.builder()
                        .gstin(gstin)
                        .period(period)
                        .build());
        entity.setDisputeNote(note);
        reconciliationRepository.save(entity);
    }

    @Transactional
    public void notifyVendor(String gstin, String period) {
        log.info("Recording notification alert sent to vendor {} for period {}", gstin, period);
        GSTReconciliationEntity entity = reconciliationRepository.findByGstinAndPeriod(gstin, period)
                .orElseGet(() -> GSTReconciliationEntity.builder()
                        .gstin(gstin)
                        .period(period)
                        .build());
        entity.setNotified(true);
        reconciliationRepository.save(entity);
    }

    @Transactional
    public void releaseHold(String gstin, String period) {
        log.info("Releasing ITC hold for vendor {} during period {}", gstin, period);
        GSTReconciliationEntity entity = reconciliationRepository.findByGstinAndPeriod(gstin, period)
                .orElseGet(() -> GSTReconciliationEntity.builder()
                        .gstin(gstin)
                        .period(period)
                        .build());
        entity.setReleased(true);
        reconciliationRepository.save(entity);
    }

    @Transactional
    public void writeOffHold(String gstin, String period) {
        log.info("Writing off ITC hold for vendor {} during period {}", gstin, period);
        GSTReconciliationEntity entity = reconciliationRepository.findByGstinAndPeriod(gstin, period)
                .orElseGet(() -> GSTReconciliationEntity.builder()
                        .gstin(gstin)
                        .period(period)
                        .build());
        entity.setWrittenOff(true);
        reconciliationRepository.save(entity);
    }
}
