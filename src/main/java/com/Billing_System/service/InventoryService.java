package com.Billing_System.service;

import com.Billing_System.dto.*;
import com.Billing_System.entity.*;
import com.Billing_System.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final BinLocationRepository binLocationRepository;
    private final StockTransferOrderRepository stoRepository;
    private final UserRepository userRepository;
    private final BlockchainAuditService auditService;

    @Transactional(readOnly = true)
    public InventorySummaryDTO getInventorySummary() {
        return InventorySummaryDTO.builder()
                .totalSkus(productRepository.countActiveProducts())
                .lowStockCount(productRepository.countLowStockProducts())
                .outOfStockCount(productRepository.countOutOfStockProducts())
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryProductDTO> getInventoryProducts(String status) {
        List<Product> products;
        if ("LOW".equalsIgnoreCase(status)) {
            products = productRepository.findLowStockProducts();
        } else if ("OUT".equalsIgnoreCase(status)) {
            products = productRepository.findOutOfStockProducts();
        } else {
            products = productRepository.findAllWithCategory();
        }

        return products.stream()
                .map(this::mapToInventoryDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private InventoryProductDTO mapToInventoryDTO(Product product) {
        String stockStatus = "OK";
        BigDecimal current = product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal min = product.getMinStock() != null ? product.getMinStock() : BigDecimal.ZERO;

        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            stockStatus = "OUT";
        } else if (current.compareTo(min) <= 0) {
            stockStatus = "LOW";
        }

        double level = 100.0;
        if (min.compareTo(BigDecimal.ZERO) > 0) {
            level = current.divide(min, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            if (level > 100.0)
                level = 100.0;
        } else if (current.compareTo(BigDecimal.ZERO) <= 0) {
            level = 0.0;
        }

        return InventoryProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Uncategorized")
                .brand(product.getBrand())
                .sellingPrice(product.getSellingPrice())
                .currentStock(current)
                .minStock(min)
                .status(stockStatus)
                .level(level)
                .build();
    }

    /**
     * Manually adjust stock levels.
     * Records a 'ADJUST' entry in the stock ledger.
     */
    @Transactional
    public void adjustStock(StockAdjustmentDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + dto.getProductId()));

        // Ensure amount is positive from DTO, we determine the sign by 'direction'
        BigDecimal amount = dto.getQuantity().abs();
        String direction = dto.getDirection() != null ? dto.getDirection().trim() : "+";

        // Logic: direction "-" or "OUT" means we decrease stock
        boolean isSubtraction = "-".equals(direction) ||
                "OUT".equalsIgnoreCase(direction) ||
                "SUBTRACT".equalsIgnoreCase(direction) ||
                "REMOVE".equalsIgnoreCase(direction);

        if (isSubtraction) {
            amount = amount.negate();
        }

        BigDecimal oldStock = product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal newStock = oldStock.add(amount);

        product.setCurrentStock(newStock);
        productRepository.save(product);

        // Record in ledger
        StockLedger ledger = StockLedger.builder()
                .product(product)
                .transactionType("ADJUST")
                .referenceId(null)
                .quantityIn(!isSubtraction ? amount : BigDecimal.ZERO)
                .quantityOut(isSubtraction ? amount.abs() : BigDecimal.ZERO)
                .balanceStock(newStock)
                .transactionDate(LocalDateTime.now())
                .reason(dto.getReason() != null ? dto.getReason() : "Manual Adjustment")
                .build();

        stockLedgerRepository.save(ledger);
    }

    @Transactional(readOnly = true)
    public List<StockLedger> getStockHistory(UUID productId) {
        return stockLedgerRepository.findByProductIdOrderByTransactionDateDesc(productId);
    }

    // --- Bin Location Management ---

    @Transactional
    public BinLocationResponseDTO createBinLocation(BinLocationRequestDTO request) {
        String fullCode = String.format("%s-%s-%d-%s", 
                request.getZone(), request.getRack(), request.getLevelNumber(), request.getBinCode()).toUpperCase();

        if (binLocationRepository.existsByBinFullCode(fullCode)) {
            throw new IllegalArgumentException("Bin Location " + fullCode + " already exists.");
        }

        BinLocation bin = BinLocation.builder()
                .zone(request.getZone().toUpperCase())
                .rack(request.getRack().toUpperCase())
                .levelNumber(request.getLevelNumber())
                .binCode(request.getBinCode().toUpperCase())
                .binFullCode(fullCode)
                .capacityUnits(request.getCapacityUnits())
                .velocityClass(request.getVelocityClass())
                .distanceFromDispatchMeters(request.getDistanceFromDispatchMeters())
                .isActive(true)
                .build();

        bin = binLocationRepository.save(bin);
        log.info("Created Bin Location: {}", fullCode);
        return mapToBinDTO(bin);
    }

    @Transactional(readOnly = true)
    public List<BinLocationResponseDTO> getAllBinLocations() {
        return binLocationRepository.findAll().stream()
                .map(this::mapToBinDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public BinLocationResponseDTO slotProductToBin(String identifier, BinSlotRequestDTO request) {
        try {
            log.info("Received slotProductToBin request. Identifier: {}, ProductId: {}, Qty: {}", 
                     identifier, request.getProductId(), request.getQuantity());

            BinLocation bin = null;
            try {
                UUID binId = UUID.fromString(identifier);
                bin = binLocationRepository.findById(binId)
                        .orElseGet(() -> binLocationRepository.findAll().stream()
                                .filter(b -> b.getBinFullCode().equalsIgnoreCase(identifier) || b.getBinCode().equalsIgnoreCase(identifier))
                                .findFirst()
                                .orElse(null));
            } catch (IllegalArgumentException e) {
                bin = binLocationRepository.findAll().stream()
                        .filter(b -> b.getBinFullCode().equalsIgnoreCase(identifier) || b.getBinCode().equalsIgnoreCase(identifier))
                        .findFirst()
                        .orElse(null);
            }

            if (bin == null) {
                String zone = "Ambient";
                String rack = "A";
                Integer level = 1;
                String binCode = identifier;
                
                try {
                    String[] parts = identifier.split("-");
                    if (parts.length >= 4) {
                        rack = parts[0];
                        String lvlStr = parts[2].replaceAll("[^0-9]", "");
                        level = Integer.parseInt(lvlStr);
                        binCode = parts[3];
                    }
                } catch (Exception ignored) {}

                bin = BinLocation.builder()
                        .binCode(binCode)
                        .binFullCode(identifier)
                        .zone(zone)
                        .rack(rack)
                        .levelNumber(level)
                        .capacityUnits(BigDecimal.valueOf(100))
                        .currentUnits(BigDecimal.ZERO)
                        .velocityClass("LOW")
                        .build();
                bin = binLocationRepository.save(bin);
                log.info("Auto-created missing BinLocation: {}", identifier);
            }

            if (request.getProductId() == null) {
                throw new IllegalArgumentException("Product ID cannot be null");
            }

            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + request.getProductId()));

            BigDecimal incomingQty = request.getQuantity();
            if (incomingQty == null) {
                throw new IllegalArgumentException("Quantity cannot be null");
            }
            if (incomingQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative");
            }

            // Validate capacity
            BigDecimal newUnits;
            if (bin.getCurrentProduct() != null && bin.getCurrentProduct().getId().equals(product.getId())) {
                newUnits = bin.getCurrentUnits().add(incomingQty);
            } else {
                // Overwriting or initial slotting
                newUnits = incomingQty;
            }

            if (newUnits.compareTo(bin.getCapacityUnits()) > 0) {
                throw new IllegalArgumentException("Cannot slot product: exceeds capacity limit of " + bin.getCapacityUnits() + " units");
            }

            bin.setCurrentProduct(product);
            bin.setCurrentUnits(newUnits);
            bin = binLocationRepository.save(bin);

            log.info("Successfully slotted product {} in bin {} (Total Units: {})", product.getName(), bin.getBinFullCode(), newUnits);
            return mapToBinDTO(bin);
        } catch (Exception e) {
            System.err.println("[SLOTTING ERROR] Exception in slotProductToBin: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // --- Stock Transfer Order (STO) Management ---

    @Transactional
    public StoResponseDTO createSTO(StoRequestDTO request, UUID userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        StockTransferOrder sto = StockTransferOrder.builder()
                .stoNumber("STO-" + System.currentTimeMillis())
                .product(product)
                .sourceBranchName(request.getSourceBranchName())
                .destBranchName(request.getDestBranchName())
                .transferQuantity(request.getTransferQuantity())
                .transferDate(request.getTransferDate())
                .status("DRAFT")
                .transferMode(request.getTransferMode())
                .priority(request.getPriority())
                .capitalSaved(request.getCapitalSaved())
                .createdBy(creator)
                .build();

        sto = stoRepository.save(sto);
        log.info("Created STO: {}", sto.getStoNumber());
        
        // 🛡️ BLOCKCHAIN AUDIT LOG 🛡️
        auditService.logAction(
            "stock_transfer_orders", 
            sto.getId(), 
            "CREATE", 
            null, 
            request, 
            userId, 
            "127.0.0.1"
        );

        return mapToStoDTO(sto);
    }

    @Transactional
    public StoResponseDTO updateSTOStatus(UUID id, String status, UUID approverId) {
        StockTransferOrder sto = stoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("STO not found"));

        sto.setStatus(status);

        if ("APPROVED".equalsIgnoreCase(status) && approverId != null) {
            User approver = userRepository.findById(approverId)
                    .orElseThrow(() -> new IllegalArgumentException("Approver not found"));
            sto.setApprovedBy(approver);
        }

        sto = stoRepository.save(sto);
        log.info("STO {} status updated to {}", sto.getStoNumber(), status);
        return mapToStoDTO(sto);
    }

    @Transactional(readOnly = true)
    public List<StoResponseDTO> getAllSTOs() {
        return stoRepository.findAll().stream()
                .map(this::mapToStoDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    // --- Mappers ---

    private BinLocationResponseDTO mapToBinDTO(BinLocation bin) {
        return BinLocationResponseDTO.builder()
                .id(bin.getId())
                .zone(bin.getZone())
                .rack(bin.getRack())
                .levelNumber(bin.getLevelNumber())
                .binCode(bin.getBinCode())
                .binFullCode(bin.getBinFullCode())
                .capacityUnits(bin.getCapacityUnits())
                .currentUnits(bin.getCurrentUnits())
                .currentProductId(bin.getCurrentProduct() != null ? bin.getCurrentProduct().getId() : null)
                .currentProductName(bin.getCurrentProduct() != null ? bin.getCurrentProduct().getName() : null)
                .velocityClass(bin.getVelocityClass())
                .distanceFromDispatchMeters(bin.getDistanceFromDispatchMeters())
                .isActive(bin.isActive())
                .build();
    }

    private StoResponseDTO mapToStoDTO(StockTransferOrder sto) {
        return StoResponseDTO.builder()
                .id(sto.getId())
                .stoNumber(sto.getStoNumber())
                .productId(sto.getProduct().getId())
                .productName(sto.getProduct().getName())
                .sourceBranchName(sto.getSourceBranchName())
                .destBranchName(sto.getDestBranchName())
                .transferQuantity(sto.getTransferQuantity())
                .transferDate(sto.getTransferDate())
                .status(sto.getStatus())
                .transferMode(sto.getTransferMode())
                .priority(sto.getPriority())
                .capitalSaved(sto.getCapitalSaved())
                .createdById(sto.getCreatedBy().getId())
                .createdByName(sto.getCreatedBy().getName())
                .approvedById(sto.getApprovedBy() != null ? sto.getApprovedBy().getId() : null)
                .approvedByName(sto.getApprovedBy() != null ? sto.getApprovedBy().getName() : null)
                .createdAt(sto.getCreatedAt())
                .build();
    }
}
