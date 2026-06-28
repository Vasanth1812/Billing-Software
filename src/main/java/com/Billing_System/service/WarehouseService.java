package com.Billing_System.service;

import com.Billing_System.dto.*;
import com.Billing_System.entity.*;
import com.Billing_System.vendor.entity.VendorCategory;
import com.Billing_System.vendor.entity.VendorProduct;
import com.Billing_System.vendor.repository.VendorCategoryRepository;
import com.Billing_System.vendor.repository.VendorProductRepository;
import com.Billing_System.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseRackRepository rackRepository;
    private final WarehouseStockRepository stockRepository;
    private final WarehouseMovementRepository movementRepository;
    private final VendorCategoryRepository vendorCategoryRepository;
    private final VendorProductRepository vendorProductRepository;

    public List<VendorCategory> getWarehouseCategories() {
        return vendorCategoryRepository.findAll();
    }

    // --- Warehouse Entity CRUD ---

    @Transactional
    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::mapToWarehouseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public WarehouseDTO getWarehouseById(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        return mapToWarehouseDTO(warehouse);
    }

    @Transactional
    public WarehouseDTO createWarehouse(WarehouseDTO dto) {
        if (dto.getCode() != null && warehouseRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Warehouse with code '" + dto.getCode() + "' already exists");
        }
        Warehouse warehouse = Warehouse.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .addressLine1(dto.getAddressLine1())
                .city(dto.getCity())
                .totalRacks(dto.getTotalRacks())
                .squareFootage(dto.getSquareFootage())
                .temperatureControlled(dto.getTemperatureControlled())
                .managerId(dto.getManagerId())
                .status(dto.getStatus() == null ? "Active" : dto.getStatus())
                .build();
        return mapToWarehouseDTO(warehouseRepository.save(warehouse));
    }

    @Transactional
    public WarehouseDTO updateWarehouse(UUID id, WarehouseDTO dto) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        
        if (dto.getCode() != null && !dto.getCode().equals(warehouse.getCode()) && warehouseRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Warehouse with code '" + dto.getCode() + "' already exists");
        }

        warehouse.setName(dto.getName());
        warehouse.setCode(dto.getCode());
        warehouse.setAddressLine1(dto.getAddressLine1());
        warehouse.setCity(dto.getCity());
        warehouse.setTotalRacks(dto.getTotalRacks());
        warehouse.setSquareFootage(dto.getSquareFootage());
        warehouse.setTemperatureControlled(dto.getTemperatureControlled());
        warehouse.setManagerId(dto.getManagerId());
        warehouse.setStatus(dto.getStatus());

        return mapToWarehouseDTO(warehouseRepository.save(warehouse));
    }

    @Transactional
    public void deleteWarehouse(UUID id) {
        warehouseRepository.deleteById(id);
    }

    private WarehouseDTO mapToWarehouseDTO(Warehouse warehouse) {
        return WarehouseDTO.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .code(warehouse.getCode())
                .addressLine1(warehouse.getAddressLine1())
                .city(warehouse.getCity())
                .totalRacks(warehouse.getTotalRacks())
                .squareFootage(warehouse.getSquareFootage())
                .temperatureControlled(warehouse.getTemperatureControlled())
                .managerId(warehouse.getManagerId())
                .status(warehouse.getStatus())
                .createdAt(warehouse.getCreatedAt())
                .updatedAt(warehouse.getUpdatedAt())
                .build();
    }
    
    // --- End Warehouse Entity CRUD ---

    public List<WarehouseRackDTO> getAllRacks() {
        if (rackRepository.count() == 0) {
            for(int i=1; i<=10; i++) {
                WarehouseRack r = new WarehouseRack();
                r.setId("R-" + String.format("%02d", i));
                rackRepository.save(r);
            }
        }
        return rackRepository.findAll().stream().map(rack -> WarehouseRackDTO.builder()
                .id(rack.getId())
                .categoryId(rack.getCategory() != null ? rack.getCategory().getId() : null)
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public WarehouseRackDTO createRack(WarehouseRackDTO dto) {
        WarehouseRack rack = new WarehouseRack();
        rack.setId(dto.getId());
        if (dto.getCategoryId() != null) {
            rack.setCategory(vendorCategoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found")));
        }
        rack = rackRepository.save(rack);
        return dto;
    }

    @Transactional
    public WarehouseRackDTO updateRackCategory(String id, UUID categoryId) {
        WarehouseRack rack = rackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rack not found"));
        rack.setCategory(vendorCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found")));
        rack = rackRepository.save(rack);
        return WarehouseRackDTO.builder()
                .id(rack.getId())
                .categoryId(rack.getCategory().getId())
                .build();
    }

    public List<WarehouseStockDTO> getAllStock() {
        return stockRepository.findAll().stream().map(stock -> WarehouseStockDTO.builder()
                .id(stock.getId())
                .productId(stock.getProduct().getId())
                .rackId(stock.getRack().getId())
                .quantity(stock.getQuantity())
                .lastUpdated(stock.getUpdatedAt() != null ? stock.getUpdatedAt() : stock.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public WarehouseStockDTO adjustStock(WarehouseStockAdjustmentDTO dto) {
        WarehouseRack rack = rackRepository.findById(dto.getRackId())
                .orElseThrow(() -> new RuntimeException("Rack not found"));
        VendorProduct product = vendorProductRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        WarehouseStock stock = stockRepository.findByProductIdAndRackId(dto.getProductId(), dto.getRackId())
                .orElse(null);

        if (stock == null) {
            if ("OUT".equalsIgnoreCase(dto.getType())) {
                throw new RuntimeException("Cannot stock out from an empty rack.");
            }
            stock = new WarehouseStock();
            stock.setProduct(product);
            stock.setRack(rack);
            stock.setQuantity(dto.getQuantity());
        } else {
            if ("IN".equalsIgnoreCase(dto.getType())) {
                stock.setQuantity(stock.getQuantity() + dto.getQuantity());
            } else if ("OUT".equalsIgnoreCase(dto.getType())) {
                if (stock.getQuantity() < dto.getQuantity()) {
                    throw new RuntimeException("Insufficient stock in rack.");
                }
                stock.setQuantity(stock.getQuantity() - dto.getQuantity());
            }
        }
        stock = stockRepository.save(stock);

        WarehouseMovement movement = new WarehouseMovement();
        movement.setProduct(product);
        movement.setRack(rack);
        movement.setMovementType(dto.getType().toUpperCase());
        movement.setQuantity(dto.getQuantity());
        movementRepository.save(movement);

        return WarehouseStockDTO.builder()
                .id(stock.getId())
                .productId(stock.getProduct().getId())
                .rackId(stock.getRack().getId())
                .quantity(stock.getQuantity())
                .lastUpdated(stock.getUpdatedAt() != null ? stock.getUpdatedAt() : stock.getCreatedAt())
                .build();
    }

    public List<WarehouseMovementDTO> getMovements() {
        return movementRepository.findAllByOrderByTimestampDesc().stream().map(m -> WarehouseMovementDTO.builder()
                .id(m.getId())
                .productId(m.getProduct().getId())
                .rackId(m.getRack().getId())
                .type(m.getMovementType())
                .quantity(m.getQuantity())
                .timestamp(m.getTimestamp())
                .build()).collect(Collectors.toList());
    }
}
