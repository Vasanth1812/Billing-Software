package com.Billing_System.controller;

import com.Billing_System.dto.ReconciliationResultDTO;
import com.Billing_System.service.AggregatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/aggregator")
@RequiredArgsConstructor
public class AggregatorController {

    private final AggregatorService aggregatorService;

    @PostMapping(value = "/reconcile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ReconciliationResultDTO>> reconcileCsv(@RequestParam("file") MultipartFile file) {
        List<ReconciliationResultDTO> results = aggregatorService.reconcileCsv(file);
        return ResponseEntity.ok(results);
    }
}
