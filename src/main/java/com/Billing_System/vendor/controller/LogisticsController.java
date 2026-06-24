package com.Billing_System.vendor.controller;

import com.Billing_System.vendor.entity.DockAppointment;
import com.Billing_System.vendor.repository.DockAppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendor/logistics")
@CrossOrigin(origins = "*")
public class LogisticsController {

    @Autowired
    private DockAppointmentRepository dockAppointmentRepository;

    @PostMapping("/appointments")
    public ResponseEntity<DockAppointment> createAppointment(@RequestBody DockAppointment appointment) {
        appointment.setStatus("Confirmed");
        DockAppointment saved = dockAppointmentRepository.save(appointment);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<DockAppointment>> getAppointments() {
        return ResponseEntity.ok(dockAppointmentRepository.findAll());
    }

    @PatchMapping("/gate-checkin")
    public ResponseEntity<DockAppointment> gateCheckin(@RequestParam UUID id, @RequestParam String action) {
        Optional<DockAppointment> opt = dockAppointmentRepository.findById(id);
        if (opt.isPresent()) {
            DockAppointment appt = opt.get();
            if ("APPROVE".equalsIgnoreCase(action)) {
                appt.setStatus("CHECKED_IN");
            } else if ("DENY".equalsIgnoreCase(action)) {
                appt.setStatus("GATE_DENY");
            } else if ("CONFIRM".equalsIgnoreCase(action)) {
                appt.setStatus("Confirmed");
            } else if ("CANCEL".equalsIgnoreCase(action)) {
                appt.setStatus("Cancelled");
            }
            return ResponseEntity.ok(dockAppointmentRepository.save(appt));
        }
        return ResponseEntity.notFound().build();
    }
}
