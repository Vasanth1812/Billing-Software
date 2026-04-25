package com.Billing_System.controller;

import com.Billing_System.dto.SystemSettingsDTO;
import com.Billing_System.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemSettingsController {

    private final SystemSettingsService service;

    @GetMapping
    public ResponseEntity<SystemSettingsDTO> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PostMapping
    public ResponseEntity<SystemSettingsDTO> updateSettings(@RequestBody SystemSettingsDTO dto) {
        return ResponseEntity.ok(service.saveSettings(dto));
    }
}
