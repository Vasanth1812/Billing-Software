package com.Billing_System.service;

import com.Billing_System.dto.SupplierDTO;
import com.Billing_System.entity.Supplier;
import com.Billing_System.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    /** Get all suppliers ordered by name */
    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findByIsActiveTrueOrderByNameAsc();
    }

    /** Get supplier by ID */
    @Transactional(readOnly = true)
    public Supplier getSupplierById(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id: " + id));
    }

    /** Create a new supplier */
    public Supplier createSupplier(SupplierDTO dto) {
        Supplier supplier = Supplier.builder()
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .gstin(dto.getGstin())
                .address(dto.getAddress())
                .creditDays(dto.getCreditDays())
                .isActive(true)
                .build();
        return supplierRepository.save(supplier);
    }

    /** Update an existing supplier */
    public Supplier updateSupplier(UUID id, SupplierDTO dto) {
        Supplier supplier = getSupplierById(id);
        supplier.setName(dto.getName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setGstin(dto.getGstin());
        supplier.setAddress(dto.getAddress());
        supplier.setCreditDays(dto.getCreditDays());
        return supplierRepository.save(supplier);
    }

    /** Delete a supplier */
    public void deleteSupplier(UUID id) {
        Supplier supplier = getSupplierById(id);
        supplierRepository.delete(supplier);
    }
}
