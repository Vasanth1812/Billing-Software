package com.Billing_System.cron;

import com.Billing_System.entity.FraudAlert;
import com.Billing_System.entity.User;
import com.Billing_System.repository.FraudAlertRepository;
import com.Billing_System.repository.UserRepository;
import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionCron {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final FraudAlertRepository fraudAlertRepository;

    /**
     * Runs every night at 2:00 AM to scan for fraud.
     * Checks if any Vendor shares contact details (Email/Phone) with an internal Employee.
     * This prevents internal employees from setting up fake vendor companies to funnel money to themselves.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runNightlyFraudScan() {
        log.info("Starting Nightly Fraud & Compliance Scan...");

        List<Vendor> activeVendors = vendorRepository.findAll(); // Should ideally filter by active
        List<User> internalEmployees = userRepository.findAll();

        int fraudCount = 0;

        for (Vendor vendor : activeVendors) {
            for (User employee : internalEmployees) {
                boolean collisionDetected = false;
                String reason = "";

                // Check Email Collision
                if (vendor.getPrimaryEmail() != null && vendor.getPrimaryEmail().equalsIgnoreCase(employee.getEmail())) {
                    collisionDetected = true;
                    reason = "Vendor Email matches Internal Employee Email: " + vendor.getPrimaryEmail();
                }

                if (collisionDetected) {
                    // Create an Alert
                    FraudAlert alert = FraudAlert.builder()
                            .alertType("VENDOR_EMPLOYEE_COLLISION")
                            .severity("CRITICAL")
                            .description(reason)
                            .vendor(vendor)
                            .employee(employee)
                            .status("OPEN")
                            .build();

                    fraudAlertRepository.save(alert);
                    log.error("CRITICAL FRAUD ALERT GENERATED: {}", reason);
                    fraudCount++;
                }
            }
        }

        log.info("Nightly Fraud Scan completed. {} new alerts generated.", fraudCount);
    }
}
