package com.Billing_System.service;

import com.Billing_System.dto.*;
import com.Billing_System.entity.*;
import com.Billing_System.repository.*;
import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.entity.VendorBankAccount;
import com.Billing_System.vendor.repository.VendorBankAccountRepository;
import com.Billing_System.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceService {

    private final VendorInvoiceRepository invoiceRepository;
    private final VendorPaymentRepository paymentRepository;
    private final VendorRepository vendorRepository;
    private final PurchaseOrderRepository poRepository;
    private final GRNRepository grnRepository;
    private final ShortageReportRepository shortageReportRepository;
    private final VendorBankAccountRepository bankAccountRepository;

    @Transactional
    public VendorInvoiceResponseDTO submitInvoice(VendorInvoiceRequestDTO request) {
        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        PurchaseOrder po = poRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));

        GRN grn = grnRepository.findById(request.getGrnId())
                .orElseThrow(() -> new IllegalArgumentException("GRN not found"));

        // Duplicate check
        if (invoiceRepository.existsByInvoiceNumberAndVendorId(request.getInvoiceNumber(), vendor.getId())) {
            throw new IllegalArgumentException("Invoice number already exists for this vendor.");
        }

        BigDecimal invoiceTotal = request.getInvoiceAmount().add(request.getGstAmount());

        // --- 3-Way Match Logic ---
        
        // 1. PO Match: Does the invoice total match the PO total?
        BigDecimal poTotal = po.getGrandTotal() != null ? po.getGrandTotal() : BigDecimal.ZERO;
        String poMatch = (invoiceTotal.compareTo(poTotal) == 0) ? "MATCHED" : 
                         (invoiceTotal.compareTo(poTotal) < 0) ? "PARTIAL" : "MISMATCHED";

        // 2. GRN Match: Calculate the exact financial value of what we received
        BigDecimal grnValue = BigDecimal.ZERO;
        for (GRNItem item : grn.getItems()) {
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            // We only pay for ACCEPTED quantity
            grnValue = grnValue.add(item.getAcceptedQuantity().multiply(unitPrice));
        }
        
        // Assume GST is roughly proportional for simplicity in this demo match
        String grnMatch = (request.getInvoiceAmount().compareTo(grnValue) == 0) ? "MATCHED" :
                          (request.getInvoiceAmount().compareTo(grnValue) < 0) ? "PARTIAL" : "MISMATCHED";

        // 3. Overall 3-Way Match boolean
        boolean threeWayMatch = "MATCHED".equals(poMatch) && "MATCHED".equals(grnMatch);

        VendorInvoice invoice = VendorInvoice.builder()
                .invoiceNumber(request.getInvoiceNumber())
                .vendor(vendor)
                .grn(grn)
                .purchaseOrder(po)
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .invoiceAmount(request.getInvoiceAmount())
                .gstAmount(request.getGstAmount())
                .totalAmount(invoiceTotal)
                .irnNumber(request.getIrnNumber())
                .poMatchStatus(poMatch)
                .grnMatchStatus(grnMatch)
                .invoiceMatchStatus("PENDING") // Will be updated by Finance team
                .threeWayMatch(threeWayMatch)
                .submissionStatus(threeWayMatch ? "APPROVED" : "UNDER_REVIEW")
                .build();

        if (threeWayMatch) {
            po.setStatus("invoiced");
            poRepository.save(po);
        }

        invoice = invoiceRepository.save(invoice);
        log.info("Vendor Invoice {} submitted. 3-Way Match: {}", invoice.getInvoiceNumber(), threeWayMatch);

        return mapToInvoiceDTO(invoice);
    }

    @Transactional(readOnly = true)
    public List<VendorInvoiceResponseDTO> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::mapToInvoiceDTO)
                .collect(Collectors.toList());
    }

    /** Bug 2 Fix: Get single invoice by ID */
    @Transactional(readOnly = true)
    public VendorInvoiceResponseDTO getInvoiceById(UUID id) {
        VendorInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        return mapToInvoiceDTO(invoice);
    }

    @Transactional(readOnly = true)
    public List<VendorPaymentResponseDTO> getAllVendorPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToPaymentDTO)
                .collect(Collectors.toList());
    }

    /** Bug 2 Fix: Get single payment by ID */
    @Transactional(readOnly = true)
    public VendorPaymentResponseDTO getPaymentById(UUID id) {
        VendorPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        return mapToPaymentDTO(payment);
    }

    @Transactional(readOnly = true)
    public List<VendorPaymentResponseDTO> getPaymentsByVendor(UUID vendorId) {
        return paymentRepository.findByVendorId(vendorId).stream()
                .map(this::mapToPaymentDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public VendorInvoiceResponseDTO approveInvoice(UUID invoiceId) {
        VendorInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        invoice.setPoMatchStatus("MATCHED");
        invoice.setGrnMatchStatus("MATCHED");
        invoice.setInvoiceMatchStatus("MATCHED");
        invoice.setThreeWayMatch(true);
        invoice.setSubmissionStatus("APPROVED");

        invoice = invoiceRepository.save(invoice);
        
        PurchaseOrder po = invoice.getPurchaseOrder();
        if (po != null) {
            po.setStatus("invoiced");
            poRepository.save(po);
        }

        log.info("Vendor Invoice {} manually approved and matched.", invoice.getInvoiceNumber());
        return mapToInvoiceDTO(invoice);
    }


    @Transactional
    public VendorPaymentResponseDTO createPayment(VendorPaymentRequestDTO request, UUID approvedByUserId) {
        VendorInvoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        VendorBankAccount bankAccount = bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Bank Account not found"));

        // Check if vendor is compliant before paying
        if ("BLOCKED".equals(vendor.getComplianceStatus()) || "NON_COMPLIANT".equals(vendor.getComplianceStatus())) {
            throw new IllegalStateException("Cannot process payment. Vendor compliance status is " + vendor.getComplianceStatus());
        }

        // Fetch Shortage Report to calculate Hold Amount
        BigDecimal holdAmount = BigDecimal.ZERO;
        String holdReason = null;
        List<ShortageReport> shortageReports = shortageReportRepository.findByGrnId(invoice.getGrn().getId());
        
        if (!shortageReports.isEmpty()) {
            ShortageReport sr = shortageReports.get(0);
            if ("OPEN".equals(sr.getStatus())) {
                holdAmount = sr.getTotalShortageValue();
                holdReason = "Debit Note applied: " + sr.getReportNumber();
                
                // Mark shortage report as resolved since we are deducting it from the payment
                sr.setStatus("RESOLVED");
                shortageReportRepository.save(sr);
            }
        }

        BigDecimal itcHoldAmount = BigDecimal.ZERO; // Placeholder for GST matching logic

        BigDecimal netPayment = request.getPaymentAmount().subtract(holdAmount).subtract(itcHoldAmount);

        VendorPayment payment = VendorPayment.builder()
                .paymentNumber("PAY-" + System.currentTimeMillis())
                .invoice(invoice)
                .vendor(vendor)
                .paymentMode(request.getPaymentMode())
                .paymentAmount(request.getPaymentAmount())
                .holdAmount(holdAmount)
                .itcHoldAmount(itcHoldAmount)
                .netPayment(netPayment)
                .holdReason(holdReason)
                .paymentDueDate(request.getPaymentDueDate() != null ? request.getPaymentDueDate() : invoice.getInvoiceDate().plusDays(30))
                .bankAccount(bankAccount)
                .status("PROCESSED")
                .build();

        payment = paymentRepository.save(payment);
        
        // Update the invoice status to PAID
        invoice.setSubmissionStatus("PAID");
        invoiceRepository.save(invoice);

        PurchaseOrder po = invoice.getPurchaseOrder();
        if (po != null) {
            po.setStatus("paid");
            poRepository.save(po);
        }

        log.info("Processed Payment {} for Vendor {}. Gross: {}, Net: {}, Hold: {}", 
                payment.getPaymentNumber(), vendor.getLegalName(), request.getPaymentAmount(), netPayment, holdAmount);

        return mapToPaymentDTO(payment);
    }

    private VendorInvoiceResponseDTO mapToInvoiceDTO(VendorInvoice invoice) {
        return VendorInvoiceResponseDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .vendorId(invoice.getVendor().getId())
                .vendorName(invoice.getVendor().getLegalName())
                .grnId(invoice.getGrn().getId())
                .grnNumber(invoice.getGrn().getGrnNumber())
                .purchaseOrderId(invoice.getPurchaseOrder().getId())
                .purchaseOrderNumber(invoice.getPurchaseOrder().getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .invoiceAmount(invoice.getInvoiceAmount())
                .gstAmount(invoice.getGstAmount())
                .totalAmount(invoice.getTotalAmount())
                .poMatchStatus(invoice.getPoMatchStatus())
                .grnMatchStatus(invoice.getGrnMatchStatus())
                .invoiceMatchStatus(invoice.getInvoiceMatchStatus())
                .threeWayMatch(invoice.isThreeWayMatch())
                .irnNumber(invoice.getIrnNumber())
                .submissionStatus(invoice.getSubmissionStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    private VendorPaymentResponseDTO mapToPaymentDTO(VendorPayment payment) {
        return VendorPaymentResponseDTO.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .invoiceId(payment.getInvoice().getId())
                .invoiceNumber(payment.getInvoice().getInvoiceNumber())
                .vendorId(payment.getVendor().getId())
                .vendorName(payment.getVendor().getLegalName())
                .paymentMode(payment.getPaymentMode())
                .paymentAmount(payment.getPaymentAmount())
                .holdAmount(payment.getHoldAmount())
                .itcHoldAmount(payment.getItcHoldAmount())
                .netPayment(payment.getNetPayment())
                .holdReason(payment.getHoldReason())
                .paymentDueDate(payment.getPaymentDueDate())
                .bankAccountId(payment.getBankAccount().getId())
                .status(payment.getStatus())
                .bankReference(payment.getBankReference())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
