package com.Billing_System.service;

import com.Billing_System.dto.BranchDTO;
import com.Billing_System.entity.Branch;
import com.Billing_System.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchDTO> getAllBranches() {
        List<Branch> branches = branchRepository.findAll();
        if (branches.isEmpty()) {
            branches = seedDefaultBranches();
        }
        return branches.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public BranchDTO createBranch(BranchDTO dto) {
        Branch branch = new Branch();
        // Generate a new ID if not provided, format BRXXX
        if (dto.getId() == null || dto.getId().isEmpty()) {
            long count = branchRepository.count() + 1;
            branch.setId(String.format("BR%03d", count));
        } else {
            branch.setId(dto.getId());
        }
        
        branch.setBranchName(dto.getBranchName());
        branch.setBranchCode(dto.getBranchCode());
        branch.setSupermarket(dto.getSupermarket());
        branch.setAddressLine1(dto.getAddressLine1());
        branch.setAddressLine2(dto.getAddressLine2());
        branch.setCity(dto.getCity());
        branch.setBranchManager(dto.getBranchManager());
        branch.setBranchManagerId(dto.getBranchManagerId());
        branch.setContactNumber(dto.getContactNumber());
        branch.setWarehouseLinked(dto.getWarehouseLinked());
        branch.setWarehouseId(dto.getWarehouseId());
        branch.setStatus(dto.getStatus() == null ? "active" : dto.getStatus());
        branch.setVendorCount(dto.getVendorCount() == null ? 0 : dto.getVendorCount());
        branch.setTotalStock(dto.getTotalStock() == null ? 0 : dto.getTotalStock());
        branch.setMonthlyRevenue(dto.getMonthlyRevenue() == null ? 0.0 : dto.getMonthlyRevenue());

        return mapToDTO(branchRepository.save(branch));
    }

    @Transactional
    public BranchDTO updateBranch(String id, BranchDTO updates) {
        Branch existing = branchRepository.findById(id).orElseThrow(() -> new RuntimeException("Branch not found"));
        
        if (updates.getBranchName() != null) existing.setBranchName(updates.getBranchName());
        if (updates.getBranchCode() != null) existing.setBranchCode(updates.getBranchCode());
        if (updates.getSupermarket() != null) existing.setSupermarket(updates.getSupermarket());
        if (updates.getAddressLine1() != null) existing.setAddressLine1(updates.getAddressLine1());
        if (updates.getAddressLine2() != null) existing.setAddressLine2(updates.getAddressLine2());
        if (updates.getCity() != null) existing.setCity(updates.getCity());
        if (updates.getBranchManager() != null) existing.setBranchManager(updates.getBranchManager());
        if (updates.getBranchManagerId() != null) existing.setBranchManagerId(updates.getBranchManagerId());
        if (updates.getContactNumber() != null) existing.setContactNumber(updates.getContactNumber());
        if (updates.getWarehouseLinked() != null) existing.setWarehouseLinked(updates.getWarehouseLinked());
        if (updates.getWarehouseId() != null) existing.setWarehouseId(updates.getWarehouseId());
        if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
        if (updates.getVendorCount() != null) existing.setVendorCount(updates.getVendorCount());
        if (updates.getTotalStock() != null) existing.setTotalStock(updates.getTotalStock());
        if (updates.getMonthlyRevenue() != null) existing.setMonthlyRevenue(updates.getMonthlyRevenue());

        return mapToDTO(branchRepository.save(existing));
    }

    @Transactional
    public void deleteBranch(String id) {
        Branch existing = branchRepository.findById(id).orElseThrow(() -> new RuntimeException("Branch not found"));
        branchRepository.delete(existing);
    }

    @Transactional
    protected List<Branch> seedDefaultBranches() {
        List<Branch> defaults = Arrays.asList(
            Branch.builder().id("BR001").branchName("Pallikarnai").branchCode("PLK001").supermarket("Crazy Supermarkets")
                .addressLine1("12, Old Pallavaram Road").addressLine2("Near Bus Stand").city("Pallikarnai")
                .branchManager("Rajesh Kumar").branchManagerId("USR001").contactNumber("+91 98765 43210")
                .warehouseLinked("Warehouse A — Pallikarnai Central").warehouseId("WH001").status("active")
                .vendorCount(24).totalStock(14520).monthlyRevenue(1850000.0).build(),
            Branch.builder().id("BR002").branchName("Red Hills").branchCode("RH001").supermarket("Crazy Supermarkets")
                .addressLine1("45, Red Hills Main Road").addressLine2("").city("Red Hills")
                .branchManager("Priya Sharma").branchManagerId("USR002").contactNumber("+91 87654 32109")
                .warehouseLinked("Warehouse B — Red Hills North").warehouseId("WH002").status("active")
                .vendorCount(18).totalStock(11230).monthlyRevenue(1420000.0).build(),
            Branch.builder().id("BR003").branchName("Tambaram").branchCode("TBM001").supermarket("Crazy Supermarkets")
                .addressLine1("78, Tambaram Sanatorium").addressLine2("GST Road").city("Tambaram")
                .branchManager("Arjun Nair").branchManagerId("USR003").contactNumber("+91 76543 21098")
                .warehouseLinked("Warehouse C — Tambaram West").warehouseId("WH003").status("active")
                .vendorCount(21).totalStock(13050).monthlyRevenue(1650000.0).build(),
            Branch.builder().id("BR004").branchName("Velachery").branchCode("VLC001").supermarket("Crazy Supermarkets")
                .addressLine1("100 Feet Road").addressLine2("Near Phoenix Mall").city("Velachery")
                .branchManager("Deepa Venkat").branchManagerId("USR004").contactNumber("+91 65432 10987")
                .warehouseLinked("").warehouseId("").status("inactive")
                .vendorCount(0).totalStock(0).monthlyRevenue(0.0).build(),
            Branch.builder().id("BR005").branchName("Anna Nagar").branchCode("AN001").supermarket("Crazy Supermarkets")
                .addressLine1("2nd Avenue, Anna Nagar").addressLine2("Block W").city("Anna Nagar")
                .branchManager("Suresh Babu").branchManagerId("USR005").contactNumber("+91 54321 09876")
                .warehouseLinked("Warehouse D — Anna Nagar Hub").warehouseId("WH004").status("active")
                .vendorCount(30).totalStock(18200).monthlyRevenue(2100000.0).build()
        );
        return branchRepository.saveAll(defaults);
    }

    private BranchDTO mapToDTO(Branch branch) {
        return BranchDTO.builder()
                .id(branch.getId())
                .branchName(branch.getBranchName())
                .branchCode(branch.getBranchCode())
                .supermarket(branch.getSupermarket())
                .addressLine1(branch.getAddressLine1())
                .addressLine2(branch.getAddressLine2())
                .city(branch.getCity())
                .branchManager(branch.getBranchManager())
                .branchManagerId(branch.getBranchManagerId())
                .contactNumber(branch.getContactNumber())
                .warehouseLinked(branch.getWarehouseLinked())
                .warehouseId(branch.getWarehouseId())
                .status(branch.getStatus())
                .vendorCount(branch.getVendorCount())
                .totalStock(branch.getTotalStock())
                .monthlyRevenue(branch.getMonthlyRevenue())
                .createdOn(branch.getCreatedOn())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
}
