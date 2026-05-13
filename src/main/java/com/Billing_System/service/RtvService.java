package com.Billing_System.service;

import com.Billing_System.dto.RtvResponseDTO;
import com.Billing_System.entity.RtvRequest;
import com.Billing_System.repository.RtvRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RtvService {

    private final RtvRequestRepository rtvRequestRepository;

    @Transactional(readOnly = true)
    public List<RtvResponseDTO> getAllRtvRequests() {
        return rtvRequestRepository.findAll().stream()
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
                .build();
    }
}
