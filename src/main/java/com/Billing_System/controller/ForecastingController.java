package com.Billing_System.controller;

import com.Billing_System.dto.DemandForecastResponseDTO;
import com.Billing_System.service.DemandForecastingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecasting")
@RequiredArgsConstructor
public class ForecastingController {

    private final DemandForecastingService demandForecastingService;

    @GetMapping("/demand")
    public ResponseEntity<DemandForecastResponseDTO> getDemandForecast(
            @RequestParam(defaultValue = "all") String storeId,
            @RequestParam(defaultValue = "all") String categoryId,
            @RequestParam(defaultValue = "") String sku,
            @RequestParam(defaultValue = "Last 12 weeks") String dateRange,
            @RequestParam(defaultValue = "5") int leadTime,
            @RequestParam(defaultValue = "1.0") double safetyMultiplier,
            @RequestParam(defaultValue = "true") boolean includeSeasonality
    ) {
        return ResponseEntity.ok(demandForecastingService.generateForecast(
                storeId, categoryId, sku, dateRange, leadTime, safetyMultiplier, includeSeasonality
        ));
    }
}
