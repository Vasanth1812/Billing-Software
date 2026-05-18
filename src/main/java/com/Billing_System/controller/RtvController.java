package com.Billing_System.controller;

import com.Billing_System.dto.RtvRequestDTO;
import com.Billing_System.dto.RtvResponseDTO;
import com.Billing_System.service.RtvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rtv")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RtvController {

    private final RtvService rtvService;

    @GetMapping
    public ResponseEntity<List<RtvResponseDTO>> getAllRtvRequests() {
        return ResponseEntity.ok(rtvService.getAllRtvRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RtvResponseDTO> getRtvById(@PathVariable UUID id) {
        return ResponseEntity.ok(rtvService.getRtvById(id));
    }

    @PostMapping
    public ResponseEntity<RtvResponseDTO> createRtvRequest(
            @RequestBody RtvRequestDTO requestDto,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(rtvService.createRtvRequest(requestDto, userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RtvResponseDTO> updateRtvStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam(required = false) String disputeNote) {
        return ResponseEntity.ok(rtvService.updateRtvStatus(id, status, disputeNote));
    }
}
