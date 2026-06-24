package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.DockAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface DockAppointmentRepository extends JpaRepository<DockAppointment, UUID> {
    List<DockAppointment> findByStoreAndDate(String store, String date);
}
