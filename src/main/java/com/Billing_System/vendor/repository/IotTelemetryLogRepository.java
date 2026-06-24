package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.IotTelemetryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IotTelemetryLogRepository extends JpaRepository<IotTelemetryLog, UUID> {
}
