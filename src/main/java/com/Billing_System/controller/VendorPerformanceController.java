package com.Billing_System.controller;

import com.Billing_System.dto.VendorScorecardDTO;
import com.Billing_System.service.VendorPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorPerformanceController {

    private final VendorPerformanceService vendorPerformanceService;

    @GetMapping("/{id}/scorecard")
    public ResponseEntity<VendorScorecardDTO> getVendorScorecard(@PathVariable UUID id) {
        return ResponseEntity.ok(vendorPerformanceService.getScorecard(id));
    }
}
