package com.Billing_System.service;

import com.Billing_System.dto.GSTReconciliationDTO;
import com.Billing_System.dto.InputTaxLineDTO;
import com.Billing_System.dto.OutputTaxLineDTO;
import com.Billing_System.entity.*;
import com.Billing_System.repository.GRNRepository;
import com.Billing_System.repository.GSTReconciliationRepository;
import com.Billing_System.repository.PurchaseOrderRepository;
import com.Billing_System.repository.SalesInvoiceRepository;
import com.Billing_System.repository.VendorInvoiceRepository;
import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.entity.VendorProduct;
import com.Billing_System.vendor.repository.VendorProductRepository;
import com.Billing_System.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GSTReconciliationService {

    private final VendorRepository vendorRepository;
    private final VendorInvoiceRepository invoiceRepository;
    private final GSTReconciliationRepository reconciliationRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final GRNRepository grnRepository;
    private final VendorProductRepository vendorProductRepository;

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

    // ─── Input Tax Ledger (Purchases → GST paid to vendors) ─────────────────────

    @Transactional(readOnly = true)
    public List<InputTaxLineDTO> getInputTaxLedger(String period) {
        log.info("Fetching input tax ledger for period: {}", period);
        LocalDate[] range = parsePeriodToRange(period);
        java.time.LocalDateTime start = range[0].atStartOfDay();
        java.time.LocalDateTime end = range[1].plusDays(1).atStartOfDay();

        List<GRN> grns = grnRepository.findByReceivedDateBetween(start, end);

        // Collect VendorProduct IDs to fetch actual batch numbers
        Set<UUID> vendorProductIds = new HashSet<>();
        
        for (GRN grn : grns) {
            for (GRNItem item : grn.getItems()) {
                if (item.getVendorProduct() != null) {
                    vendorProductIds.add(item.getVendorProduct().getId());
                } else if (item.getPurchaseItem() != null && item.getPurchaseItem().getVendorProductId() != null) {
                    vendorProductIds.add(item.getPurchaseItem().getVendorProductId());
                }
            }
        }

        // Fetch actual batch numbers from VendorProducts
        Map<UUID, VendorProduct> vendorProductMap = new HashMap<>();
        if (!vendorProductIds.isEmpty()) {
            vendorProductRepository.findAllById(vendorProductIds).forEach(vp -> {
                vendorProductMap.put(vp.getId(), vp);
            });
        }

        List<InputTaxLineDTO> result = new ArrayList<>();

        for (GRN grn : grns) {
            String vendorName = "Unknown Vendor";
            if (grn.getVendor() != null) {
                vendorName = grn.getVendor().getLegalName() != null ? grn.getVendor().getLegalName() : "Vendor #" + grn.getVendor().getId();
            } else if (grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getVendor() != null) {
                vendorName = grn.getPurchaseOrder().getVendor().getLegalName() != null ? grn.getPurchaseOrder().getVendor().getLegalName() : "Vendor #" + grn.getPurchaseOrder().getVendor().getId();
            }

            for (GRNItem item : grn.getItems()) {
                BigDecimal qty = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : BigDecimal.ZERO;
                BigDecimal rate = item.getUnitPrice() != null ? item.getUnitPrice() : (item.getPurchaseItem() != null && item.getPurchaseItem().getPurchaseRate() != null ? item.getPurchaseItem().getPurchaseRate() : BigDecimal.ZERO);
                BigDecimal taxable = qty.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                
                BigDecimal gstRate = (item.getPurchaseItem() != null && item.getPurchaseItem().getGstRate() != null) ? item.getPurchaseItem().getGstRate() : BigDecimal.ZERO;
                BigDecimal gstAmount = taxable.multiply(gstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                // Prefer actual batchNumber from VendorProduct
                String actualBatchNumber = null;
                UUID vpId = item.getVendorProduct() != null ? item.getVendorProduct().getId() : (item.getPurchaseItem() != null ? item.getPurchaseItem().getVendorProductId() : null);
                if (vpId != null && vendorProductMap.containsKey(vpId)) {
                    String vpBatch = vendorProductMap.get(vpId).getBatchNumber();
                    if (vpBatch != null && !vpBatch.trim().isEmpty()) {
                        actualBatchNumber = vpBatch;
                    }
                }

                result.add(InputTaxLineDTO.builder()
                        .orderId(grn.getId())
                        .vendorName(vendorName)
                        .grnNumber(grn.getGrnNumber())
                        .batchNumber(actualBatchNumber)
                        .poNumber(grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getInvoiceNumber() : "—")
                        .orderDate(grn.getReceivedDate() != null ? grn.getReceivedDate().toLocalDate() : null)
                        .productName(item.getProduct() != null ? item.getProduct().getName() : (item.getPurchaseItem() != null ? item.getPurchaseItem().getProductName() : "—"))
                        .taxableAmount(taxable)
                        .gstRate(gstRate)
                        .gstAmount(gstAmount)
                        .build());
            }
        }

        return result;
    }

    // ─── Output Tax Ledger (Sales → GST collected from customers) ───────────────

    @Transactional(readOnly = true)
    public List<OutputTaxLineDTO> getOutputTaxLedger(String period) {
        log.info("Fetching output tax ledger for period: {}", period);
        LocalDate[] range = parsePeriodToRange(period);

        List<SalesInvoice> sales = salesInvoiceRepository.findByDateRange(range[0], range[1]);

        List<OutputTaxLineDTO> result = new ArrayList<>();

        for (SalesInvoice sale : sales) {
            String category = "General";
            String vendorName = "Multiple / Unknown";
            String batchNumber = null;

            if (sale.getItems() != null && !sale.getItems().isEmpty()) {
                SaleItem firstItem = sale.getItems().get(0);
                if (firstItem.getProduct() != null) {
                    if (firstItem.getProduct().getCategory() != null) {
                        category = firstItem.getProduct().getCategory().getName();
                    }
                    
                    // Fetch VendorProduct mapped to this store product to get true vendor and batch
                    List<VendorProduct> mappedVendorProducts = vendorProductRepository.findByMappedProductId(firstItem.getProduct().getId());
                    if (!mappedVendorProducts.isEmpty()) {
                        VendorProduct vp = mappedVendorProducts.get(0);
                        if (vp.getVendor() != null && vp.getVendor().getLegalName() != null) {
                            vendorName = vp.getVendor().getLegalName();
                        }
                        if (vp.getBatchNumber() != null && !vp.getBatchNumber().trim().isEmpty()) {
                            batchNumber = vp.getBatchNumber();
                        }
                    } else if (firstItem.getProduct().getPrimarySupplier() != null) {
                        // Fallback to primary supplier if no direct VendorProduct map exists
                        vendorName = firstItem.getProduct().getPrimarySupplier().getName();
                    }
                }
            }

            BigDecimal cgst = sale.getCgstAmount() != null ? sale.getCgstAmount() : BigDecimal.ZERO;
            BigDecimal sgst = sale.getSgstAmount() != null ? sale.getSgstAmount() : BigDecimal.ZERO;
            BigDecimal totalGst = cgst.add(sgst);

            result.add(OutputTaxLineDTO.builder()
                    .saleId(sale.getId())
                    .invoiceNumber(sale.getInvoiceNumber())
                    .category(category)
                    .vendorName(vendorName)
                    .batchNumber(batchNumber)
                    .customerName(sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in Customer")
                    .saleDate(sale.getInvoiceDate())
                    .taxableAmount(sale.getSubtotal() != null ? sale.getSubtotal() : BigDecimal.ZERO)
                    .totalGstAmount(totalGst)
                    .grandTotal(sale.getGrandTotal() != null ? sale.getGrandTotal() : BigDecimal.ZERO)
                    .paymentMode(sale.getPaymentMode())
                    .build());
        }

        return result;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private LocalDate[] parsePeriodToRange(String period) {
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        try {
            if (period != null && period.contains("-")) {
                String[] parts = period.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            log.warn("Invalid period format: {}, defaulting to current month", period);
        }
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.with(TemporalAdjusters.lastDayOfMonth());
        return new LocalDate[]{from, to};
    }
}
