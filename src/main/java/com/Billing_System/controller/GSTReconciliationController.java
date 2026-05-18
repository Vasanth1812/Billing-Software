package com.Billing_System.controller;

import com.Billing_System.dto.GSTReconciliationDTO;
import com.Billing_System.service.GSTReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gst/reconciliation")
@RequiredArgsConstructor
public class GSTReconciliationController {

    private final GSTReconciliationService gstReconciliationService;

    @GetMapping
    public ResponseEntity<List<GSTReconciliationDTO>> getReconciliationData(@RequestParam(required = false, defaultValue = "2026-04") String period) {
        return ResponseEntity.ok(gstReconciliationService.getReconciliationData(period));
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> notifyVendor(@RequestParam String gstin, @RequestParam String period) {
        gstReconciliationService.notifyVendor(gstin, period);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/release")
    public ResponseEntity<Void> releaseHold(@RequestParam String gstin, @RequestParam String period) {
        gstReconciliationService.releaseHold(gstin, period);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/write-off")
    public ResponseEntity<Void> writeOffHold(@RequestParam String gstin, @RequestParam String period) {
        gstReconciliationService.writeOffHold(gstin, period);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/dispute-note")
    public ResponseEntity<Void> updateDisputeNote(@RequestParam String gstin, @RequestParam String period, @RequestParam String note) {
        gstReconciliationService.updateDisputeNote(gstin, period, note);
        return ResponseEntity.ok().build();
    }
}
