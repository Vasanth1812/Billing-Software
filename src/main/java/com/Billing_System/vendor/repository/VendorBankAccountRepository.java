package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.VendorBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorBankAccountRepository extends JpaRepository<VendorBankAccount, UUID> {
    List<VendorBankAccount> findByVendorIdOrderByIsPrimaryDesc(UUID vendorId);
    boolean existsByAccountNumberHash(String accountNumberHash);
    void deleteByVendorId(UUID vendorId);
}
