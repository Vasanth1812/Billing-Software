package com.Billing_System.service;

import com.Billing_System.dto.HeldSaleDTO;
import com.Billing_System.entity.HeldSale;
import com.Billing_System.repository.HeldSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeldSaleService {

    private final HeldSaleRepository heldSaleRepository;

    @Transactional
    public HeldSaleDTO holdSale(HeldSaleDTO dto) {
        HeldSale entity = HeldSale.builder()
                .label(dto.getLabel())
                .itemsJson(dto.getItemsJson())
                .amount(dto.getAmount())
                .userId(dto.getUserId())
                .build();

        HeldSale saved = heldSaleRepository.save(entity);
        return mapToDTO(saved);
    }

    public List<HeldSaleDTO> getHeldSales(UUID userId) {
        List<HeldSale> list;
        if (userId != null) {
            list = heldSaleRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else {
            list = heldSaleRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteHeldSale(UUID id) {
        heldSaleRepository.deleteById(id);
    }

    @Transactional
    public void clearAllHeldSales() {
        heldSaleRepository.deleteAll();
    }

    private HeldSaleDTO mapToDTO(HeldSale entity) {
        return HeldSaleDTO.builder()
                .id(entity.getId())
                .label(entity.getLabel())
                .itemsJson(entity.getItemsJson())
                .amount(entity.getAmount())
                .userId(entity.getUserId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
