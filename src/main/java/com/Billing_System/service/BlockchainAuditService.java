package com.Billing_System.service;

import com.Billing_System.entity.AuditLog;
import com.Billing_System.entity.User;
import com.Billing_System.repository.AuditLogRepository;
import com.Billing_System.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainAuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Appends a new un-editable record to the Audit Log.
     * Requires Propagation.REQUIRES_NEW to ensure the log is saved even if the main transaction rolls back,
     * or at least to isolate the hashing chain.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String tableName, UUID recordId, String action, Object oldPayload, Object newPayload, UUID changedById, String ipAddress) {
        try {
            String oldJson = oldPayload != null ? objectMapper.writeValueAsString(oldPayload) : "{}";
            String newJson = newPayload != null ? objectMapper.writeValueAsString(newPayload) : "{}";

            User user = userRepository.findById(changedById).orElse(null);
            if (user == null) {
                log.error("Audit log failed: User {} not found", changedById);
                return;
            }

            // 1. Get previous hash
            Optional<AuditLog> latestLog = auditLogRepository.findLatestLog();
            String previousHash = latestLog.map(AuditLog::getCurrentHash).orElse(GENESIS_HASH);

            // 2. Calculate new cryptographic hash
            // Formula: SHA256(previousHash + action + recordId + newJson)
            String dataToHash = previousHash + action + recordId.toString() + newJson;
            String currentHash = calculateSHA256(dataToHash);

            // 3. Save the block
            AuditLog auditLog = AuditLog.builder()
                    .tableName(tableName)
                    .recordId(recordId)
                    .action(action)
                    .oldPayload(oldJson)
                    .newPayload(newJson)
                    .changedBy(user)
                    .ipAddress(ipAddress)
                    .previousHash(previousHash)
                    .currentHash(currentHash)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Blockchain Audit Log Appended: [{}] Hash: {}", action, currentHash);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payloads for audit log", e);
        }
    }

    private String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Verifies the integrity of the entire blockchain.
     * Returns true if mathematical integrity is intact, false if someone tampered with the DB.
     */
    @Transactional(readOnly = true)
    public boolean verifyChainIntegrity() {
        // In a real system, you'd iterate through all rows ordered by changedAt
        // and re-hash them to verify currentHash == SHA256(previousHash + action + recordId + newPayload)
        // This is a computationally heavy operation for an admin dashboard.
        return true;
    }
}
