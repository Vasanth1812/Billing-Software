package com.Billing_System.vendor.controller;

import com.Billing_System.vendor.entity.GoodsReceiptNote;
import com.Billing_System.vendor.entity.GrnLineItem;
import com.Billing_System.vendor.entity.DockAppointment;
import com.Billing_System.vendor.repository.GoodsReceiptNoteRepository;
import com.Billing_System.vendor.repository.DockAppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vendor/logistics/receiving")
@CrossOrigin(origins = "*")
public class ReceivingController {

    @Autowired
    private GoodsReceiptNoteRepository grnRepository;
    
    @Autowired
    private DockAppointmentRepository dockAppointmentRepository;

    @PostMapping("/submit")
    public ResponseEntity<GoodsReceiptNote> submitGrn(@RequestBody GoodsReceiptNote grn) {
        boolean hasShortage = false;

        if (grn.getLineItems() != null) {
            for (GrnLineItem item : grn.getLineItems()) {
                item.setGrn(grn);
                
                if (item.getExpectedQty() != null && item.getScannedQty() != null) {
                    if (item.getScannedQty() < item.getExpectedQty()) {
                        hasShortage = true;
                    }
                }
                if (Boolean.TRUE.equals(item.getIsDamaged()) || Boolean.TRUE.equals(item.getIsRejected())) {
                    hasShortage = true;
                }
            }
        }

        if (hasShortage) {
            grn.setStatus("SHORTAGE_ALERT");
        } else {
            grn.setStatus("RECEIVED");
        }

        GoodsReceiptNote saved = grnRepository.save(grn);
        
        // Update corresponding DockAppointment status
        if (grn.getPoNumber() != null) {
            List<DockAppointment> appts = dockAppointmentRepository.findAll();
            for (DockAppointment appt : appts) {
                if (grn.getPoNumber().equals(appt.getPoNumbers()) || grn.getPoNumber().equals(appt.getId().toString())) {
                    appt.setStatus("Received");
                    dockAppointmentRepository.save(appt);
                    break;
                }
            }
        }
        
        return ResponseEntity.ok(saved);
    }
}
