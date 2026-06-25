package com.Billing_System.service;

import com.Billing_System.dto.ReconciliationResultDTO;
import com.Billing_System.entity.VendorInvoice;
import com.Billing_System.repository.VendorInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AggregatorService {

    private final VendorInvoiceRepository vendorInvoiceRepository;

    public List<ReconciliationResultDTO> reconcileCsv(MultipartFile file) {
        List<ReconciliationResultDTO> results = new ArrayList<>();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (Row row : sheet) {
                        if (row.getRowNum() == 0) continue; // skip header
                        
                        Cell invCell = row.getCell(0);
                        Cell vendorCell = row.getCell(1);
                        Cell dateCell = row.getCell(2);
                        Cell amtCell = row.getCell(3);
                        
                        if (invCell == null || amtCell == null) continue;
                        
                        String invoiceNumber = getCellValueAsString(invCell).trim();
                        if (invoiceNumber.isEmpty()) continue;
                        
                        String vendorName = vendorCell != null ? getCellValueAsString(vendorCell).trim() : "";
                        String date = dateCell != null ? getCellValueAsString(dateCell).trim() : "";
                        String amtStr = getCellValueAsString(amtCell).replaceAll("[^0-9.]", "");
                        if (amtStr.isEmpty()) continue;
                        BigDecimal theirs = new BigDecimal(amtStr);

                        processRecord(invoiceNumber, vendorName, date, theirs, results);
                    }
                }
            } else {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    String headerLine = reader.readLine(); 
                    if (headerLine == null) throw new RuntimeException("Empty file");
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        String[] columns = line.split("[,;\\t]", -1);
                        
                        if (columns.length < 4) continue;
                        
                        String invoiceNumber = columns[0].trim();
                        String vendorName = columns[1].trim();
                        String date = columns[2].trim();
                        String amtStr = columns[3].trim().replaceAll("[^0-9.]", "");
                        if (invoiceNumber.isEmpty() || amtStr.isEmpty()) continue;
                        
                        BigDecimal theirs = new BigDecimal(amtStr);
                        processRecord(invoiceNumber, vendorName, date, theirs, results);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing file", e);
            throw new RuntimeException("Failed to parse the uploaded file.");
        }
        
        return results;
    }

    private void processRecord(String invoiceNumber, String vendorName, String date, BigDecimal theirs, List<ReconciliationResultDTO> results) {
        Optional<VendorInvoice> optionalInvoice = vendorInvoiceRepository.findByInvoiceNumber(invoiceNumber);
        
        if (optionalInvoice.isPresent()) {
            VendorInvoice invoice = optionalInvoice.get();
            BigDecimal our = invoice.getTotalAmount();
            boolean match = our.compareTo(theirs) == 0;
            
            results.add(ReconciliationResultDTO.builder()
                    .id(invoiceNumber)
                    .vendor(vendorName != null && !vendorName.isEmpty() ? vendorName : invoice.getVendor().getLegalName())
                    .date(date != null && !date.isEmpty() ? date : invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                    .our(our)
                    .theirs(theirs)
                    .match(match)
                    .build());
        } else {
            results.add(ReconciliationResultDTO.builder()
                    .id(invoiceNumber)
                    .vendor(vendorName)
                    .date(date)
                    .our(BigDecimal.ZERO)
                    .theirs(theirs)
                    .match(false)
                    .build());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: 
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }
}
