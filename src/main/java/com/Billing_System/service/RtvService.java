package com.Billing_System.service;

import com.Billing_System.dto.RtvRequestDTO;
import com.Billing_System.dto.RtvResponseDTO;
import com.Billing_System.entity.GRN;
import com.Billing_System.entity.RtvRequest;
import com.Billing_System.entity.User;
import com.Billing_System.repository.GRNRepository;
import com.Billing_System.repository.RtvRequestRepository;
import com.Billing_System.repository.UserRepository;
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
public class RtvService {

    private final RtvRequestRepository rtvRequestRepository;
    private final GRNRepository grnRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RtvResponseDTO> getAllRtvRequests() {
        return rtvRequestRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RtvResponseDTO> getRtvRequestsByVendor(UUID vendorId) {
        return rtvRequestRepository.findByVendorId(vendorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RtvResponseDTO getRtvById(UUID id) {
        RtvRequest rtv = rtvRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RTV Request not found"));
        return mapToDTO(rtv);
    }

    @Transactional
    public RtvResponseDTO createRtvRequest(RtvRequestDTO dto, UUID userId) {
        log.info("Initiating dynamic Return-to-Vendor request for GRN ID: {}", dto.getGrnId());

        GRN grn = grnRepository.findById(dto.getGrnId())
                .orElseThrow(() -> new IllegalArgumentException("Linked GRN not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Active user not found"));

        String rtvNumber = "RTV-" + (System.currentTimeMillis() % 10000000);

        RtvRequest rtv = RtvRequest.builder()
                .rtvNumber(rtvNumber)
                .grn(grn)
                .purchaseOrder(grn.getPurchaseOrder())
                .vendor(grn.getVendor())
                .status(dto.getStatus() != null ? dto.getStatus() : "DEBIT_NOTE_RAISED")
                .totalReturnValue(dto.getTotalReturnValue() != null ? dto.getTotalReturnValue() : BigDecimal.ZERO)
                .createdBy(user)
                .build();

        rtv = rtvRequestRepository.save(rtv);
        log.info("Return request {} initiated successfully for vendor {}", rtv.getRtvNumber(), grn.getVendor().getLegalName());

        return mapToDTO(rtv);
    }

    @Transactional
    public RtvResponseDTO updateRtvStatus(UUID id, String status, String disputeNote) {
        RtvRequest rtv = rtvRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RTV Request not found"));

        // Prevent reverting from a closed state
        if ("RESOLVED".equalsIgnoreCase(rtv.getStatus()) || "FORCE_CLOSED".equalsIgnoreCase(rtv.getStatus())) {
            throw new IllegalStateException("Cannot change status of a closed RTV Request.");
        }

        // If someone accidentally disputes and reverts back to SHIPPED_BACK, clear the note
        if ("SHIPPED_BACK".equalsIgnoreCase(status) && "DISPUTED".equalsIgnoreCase(rtv.getStatus())) {
            rtv.setDisputeNote(null);
        }

        rtv.setStatus(status);
        
        if (disputeNote != null && !disputeNote.isEmpty()) {
            rtv.setDisputeNote(disputeNote);
        }

        if ("RESOLVED".equalsIgnoreCase(status) || "FORCE_CLOSED".equalsIgnoreCase(status)) {
            rtv.setResolvedAt(LocalDateTime.now());
        }

        rtv = rtvRequestRepository.save(rtv);
        log.info("RTV Request {} status updated to {}", rtv.getRtvNumber(), status);
        
        return mapToDTO(rtv);
    }

    private RtvResponseDTO mapToDTO(RtvRequest rtv) {
        return RtvResponseDTO.builder()
                .id(rtv.getId())
                .rtvNumber(rtv.getRtvNumber())
                .grnId(rtv.getGrn().getId())
                .grnNumber(rtv.getGrn().getGrnNumber())
                .purchaseOrderId(rtv.getPurchaseOrder().getId())
                .purchaseOrderNumber(rtv.getPurchaseOrder().getInvoiceNumber())
                .vendorId(rtv.getVendor().getId())
                .vendorName(rtv.getVendor().getLegalName())
                .status(rtv.getStatus())
                .totalReturnValue(rtv.getTotalReturnValue())
                .shortageReportId(rtv.getShortageReport() != null ? rtv.getShortageReport().getId() : null)
                .shortageReportNumber(rtv.getShortageReport() != null ? rtv.getShortageReport().getReportNumber() : null)
                .disputeNote(rtv.getDisputeNote())
                .resolvedAt(rtv.getResolvedAt())
                .createdById(rtv.getCreatedBy().getId())
                .createdByName(rtv.getCreatedBy().getName())
                .createdAt(rtv.getCreatedAt())
                .returnedProducts(
                        rtv.getGrn().getItems().stream()
                                .filter(item -> item.getRejectedQuantity() != null && item.getRejectedQuantity().compareTo(BigDecimal.ZERO) > 0)
                                .map(item -> com.Billing_System.dto.RtvProductDTO.builder()
                                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                                        .productName(item.getProduct() != null ? item.getProduct().getName() : (item.getVendorProduct() != null ? item.getVendorProduct().getProductName() : "Unknown"))
                                        .vendorSku(item.getVendorProduct() != null ? item.getVendorProduct().getVendorSku() : null)
                                        .batchNumber(item.getVendorProduct() != null ? item.getVendorProduct().getBatchNumber() : null)
                                        .returnedQuantity(item.getRejectedQuantity())
                                        .unitPrice(item.getUnitPrice())
                                        .totalValue(item.getRejectedQuantity().multiply(item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO))
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }
}
