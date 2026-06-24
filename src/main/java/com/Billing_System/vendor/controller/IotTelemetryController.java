package com.Billing_System.vendor.controller;

import com.Billing_System.vendor.entity.IotTelemetryLog;
import com.Billing_System.vendor.repository.IotTelemetryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/logistics/iot")
@CrossOrigin(origins = "*")
public class IotTelemetryController {

    @Autowired
    private IotTelemetryLogRepository iotTelemetryLogRepository;

    @PostMapping("/telemetry")
    public ResponseEntity<IotTelemetryLog> recordTelemetry(@RequestBody IotTelemetryLog log) {
        
        if (log.getTemperature() != null && log.getTemperature() > 8.5) {
            log.setIsBreach(true);
            log.setStatus("REJECTED_COMPLIANCE_BREACH");
        } else {
            log.setIsBreach(false);
            log.setStatus("LOGGED");
        }

        IotTelemetryLog saved = iotTelemetryLogRepository.save(log);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/override")
    public ResponseEntity<IotTelemetryLog> managerOverride(@RequestParam UUID id) {
        return iotTelemetryLogRepository.findById(id).map(log -> {
            log.setStatus("OVERRIDDEN");
            return ResponseEntity.ok(iotTelemetryLogRepository.save(log));
        }).orElse(ResponseEntity.notFound().build());
    }
}
