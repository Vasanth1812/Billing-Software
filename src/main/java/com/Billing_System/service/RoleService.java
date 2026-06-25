package com.Billing_System.service;

import com.Billing_System.dto.RoleDTO;
import com.Billing_System.entity.Role;
import com.Billing_System.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        if (roles.isEmpty()) {
            roles = seedDefaultRoles();
        }
        return roles.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public RoleDTO createRole(RoleDTO dto) {
        Role role = new Role();
        role.setId(dto.getId() == null || dto.getId().isEmpty() ? "role_" + System.currentTimeMillis() : dto.getId());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setColor(dto.getColor());
        role.setIsSystem(false);
        role.setMemberCount(dto.getMemberCount() == null ? 0 : dto.getMemberCount());
        role.setPermissions(dto.getPermissions());
        return mapToDTO(roleRepository.save(role));
    }

    @Transactional
    public RoleDTO updateRole(String id, RoleDTO updates) {
        Role existing = roleRepository.findById(id).orElseThrow(() -> new RuntimeException("Role not found"));
        
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getColor() != null) existing.setColor(updates.getColor());
        if (updates.getPermissions() != null) existing.setPermissions(updates.getPermissions());
        
        return mapToDTO(roleRepository.save(existing));
    }

    @Transactional
    public void deleteRole(String id) {
        Role existing = roleRepository.findById(id).orElseThrow(() -> new RuntimeException("Role not found"));
        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new RuntimeException("Cannot delete a system role");
        }
        roleRepository.delete(existing);
    }

    @Transactional
    public RoleDTO duplicateRole(String id) {
        Role existing = roleRepository.findById(id).orElseThrow(() -> new RuntimeException("Role not found"));
        Role copy = new Role();
        copy.setId("role_" + System.currentTimeMillis());
        copy.setName(existing.getName() + " (Copy)");
        copy.setDescription(existing.getDescription());
        copy.setColor(existing.getColor());
        copy.setIsSystem(false);
        copy.setMemberCount(0);
        copy.setPermissions(existing.getPermissions() != null ? List.copyOf(existing.getPermissions()) : null);
        return mapToDTO(roleRepository.save(copy));
    }

    @Transactional
    protected List<Role> seedDefaultRoles() {
        List<Role> defaults = Arrays.asList(
            Role.builder().id("role_admin").name("Admin (Live DB)").description("Full system access with all permissions.").color("#166534").isSystem(true).memberCount(2).permissions(Arrays.asList("dashboard", "vendorList", "vendorProducts", "purchaseOrders", "grnManagement", "invoices", "payables", "returnsClaims", "gstReconciliation", "fulfillment", "reportsHub", "vendorPortal", "multiOutlet", "warehouseMap", "stockTransfer", "cycleAudit", "approvalQueue", "smartPO", "forecasting", "liveAuction", "inboundLogistics", "aggregatorPayout", "vendorSettings", "roleManagement")).build(),
            Role.builder().id("role_manager").name("Manager (Live DB)").description("Can manage procurement, vendors, and view reports. No admin access.").color("#0284c7").isSystem(false).memberCount(5).permissions(Arrays.asList("dashboard", "vendorList", "vendorProducts", "purchaseOrders", "grnManagement", "invoices", "payables", "returnsClaims", "fulfillment", "reportsHub", "vendorPortal", "multiOutlet", "approvalQueue", "smartPO", "forecasting")).build(),
            Role.builder().id("role_cashier").name("Cashier (Live DB)").description("Limited access for billing and payment operations.").color("#d97706").isSystem(false).memberCount(8).permissions(Arrays.asList("dashboard", "invoices", "payables", "gstReconciliation")).build(),
            Role.builder().id("role_viewer").name("Viewer (Live DB)").description("Read-only access to dashboards and reports.").color("#7c3aed").isSystem(false).memberCount(12).permissions(Arrays.asList("dashboard", "reportsHub", "forecasting")).build()
        );
        return roleRepository.saveAll(defaults);
    }

    private RoleDTO mapToDTO(Role role) {
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .color(role.getColor())
                .isSystem(role.getIsSystem())
                .memberCount(role.getMemberCount())
                .permissions(role.getPermissions())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
