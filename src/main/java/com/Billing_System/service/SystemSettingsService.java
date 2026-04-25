package com.Billing_System.service;

import com.Billing_System.dto.SystemSettingsDTO;
import com.Billing_System.entity.SystemSettings;
import com.Billing_System.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private final SystemSettingsRepository repository;

    public SystemSettingsDTO getSettings() {
        List<SystemSettings> settingsList = repository.findAll();
        if (settingsList.isEmpty()) {
            return new SystemSettingsDTO();
        }
        return convertToDTO(settingsList.get(0));
    }

    @Transactional
    public SystemSettingsDTO saveSettings(SystemSettingsDTO dto) {
        List<SystemSettings> settingsList = repository.findAll();
        SystemSettings settings;

        if (settingsList.isEmpty()) {
            settings = new SystemSettings();
        } else {
            settings = settingsList.get(0);
        }

        settings.setBusinessName(dto.getBusinessName());
        settings.setGstin(dto.getGstin());
        settings.setAddress(dto.getAddress());
        settings.setPhone(dto.getPhone());
        settings.setEmail(dto.getEmail());
        settings.setBankDetails(dto.getBankDetails());
        settings.setStateCode(dto.getStateCode());
        settings.setState(dto.getState());
        settings.setAutoBackup(dto.getAutoBackup());
        settings.setBackupEmail(dto.getBackupEmail());
        settings.setUpdatedAt(LocalDateTime.now());

        SystemSettings saved = repository.save(settings);
        return convertToDTO(saved);
    }

    private SystemSettingsDTO convertToDTO(SystemSettings entity) {
        return SystemSettingsDTO.builder()
                .id(entity.getId())
                .businessName(entity.getBusinessName())
                .gstin(entity.getGstin())
                .address(entity.getAddress())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .bankDetails(entity.getBankDetails())
                .stateCode(entity.getStateCode())
                .state(entity.getState())
                .autoBackup(entity.getAutoBackup())
                .backupEmail(entity.getBackupEmail())
                .build();
    }
}
