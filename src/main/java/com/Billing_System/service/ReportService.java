package com.Billing_System.service;

import com.Billing_System.dto.GstSlabSummaryDTO;
import com.Billing_System.dto.GstSummaryDTO;
import com.Billing_System.entity.PurchaseItem;
import com.Billing_System.entity.PurchaseOrder;
import com.Billing_System.entity.SaleItem;
import com.Billing_System.entity.SalesInvoice;
import com.Billing_System.repository.PurchaseOrderRepository;
import com.Billing_System.repository.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional(readOnly = true)
    public GstSummaryDTO getGstSummary(LocalDate from, LocalDate to) {
        List<SalesInvoice> sales = salesInvoiceRepository.findByDateRange(from, to);
        List<PurchaseOrder> purchases = purchaseOrderRepository.findByDateRange(from, to);

        // Initialize slab summaries with standard rates
        Map<BigDecimal, GstSlabSummaryDTO> slabMap = new LinkedHashMap<>();
        BigDecimal[] standardSlabs = {
                BigDecimal.ZERO,
                new BigDecimal("5"),
                new BigDecimal("12"),
                new BigDecimal("18"),
                new BigDecimal("28")
        };

        for (BigDecimal slab : standardSlabs) {
            BigDecimal key = slab.setScale(2, RoundingMode.HALF_UP);
            slabMap.put(key, GstSlabSummaryDTO.builder()
                    .gstSlab(slab.intValue() + "%")
                    .outputTaxable(BigDecimal.ZERO)
                    .cgst(BigDecimal.ZERO)
                    .sgst(BigDecimal.ZERO)
                    .totalOutputGst(BigDecimal.ZERO)
                    .inputTaxable(BigDecimal.ZERO)
                    .cgstItc(BigDecimal.ZERO)
                    .sgstItc(BigDecimal.ZERO)
                    .totalInputGst(BigDecimal.ZERO)
                    .netPayable(BigDecimal.ZERO)
                    .build());
        }

        // Aggregate Sales (Output GST)
        for (SalesInvoice si : sales) {
            for (SaleItem item : si.getItems()) {
                BigDecimal rate = item.getGstRate() != null ? item.getGstRate().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                GstSlabSummaryDTO dto = slabMap.get(rate);

                if (dto == null) {
                    dto = createEmptySlab(rate);
                    slabMap.put(rate, dto);
                }

                BigDecimal gst = item.getGstAmount() != null ? item.getGstAmount() : BigDecimal.ZERO;
                BigDecimal net = item.getNetAmount() != null ? item.getNetAmount() : BigDecimal.ZERO;
                BigDecimal taxable = net.subtract(gst);

                dto.setOutputTaxable(dto.getOutputTaxable().add(taxable));
                dto.setTotalOutputGst(dto.getTotalOutputGst().add(gst));

                // Use invoice level CGST/SGST if it's the only item? No, better split item
                // level GST
                // standard 50/50 split as per Indian rules unless IGST which we'll treat as
                // total for now or split if needed
                // The UI shows CGST and SGST columns.
                BigDecimal halfGst = gst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                dto.setCgst(dto.getCgst().add(halfGst));
                dto.setSgst(dto.getSgst().add(halfGst));
            }
        }

        // Aggregate Purchases (Input GST / ITC)
        for (PurchaseOrder po : purchases) {
            for (PurchaseItem item : po.getItems()) {
                BigDecimal rate = item.getGstRate() != null ? item.getGstRate().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                GstSlabSummaryDTO dto = slabMap.get(rate);

                if (dto == null) {
                    dto = createEmptySlab(rate);
                    slabMap.put(rate, dto);
                }

                BigDecimal gst = item.getGstAmount() != null ? item.getGstAmount() : BigDecimal.ZERO;
                BigDecimal total = item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal taxable = total.subtract(gst);

                dto.setInputTaxable(dto.getInputTaxable().add(taxable));
                dto.setTotalInputGst(dto.getTotalInputGst().add(gst));

                BigDecimal halfGst = gst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                dto.setCgstItc(dto.getCgstItc().add(halfGst));
                dto.setSgstItc(dto.getSgstItc().add(halfGst));
            }
        }

        // Calculate Net Payable per slab and overall totals
        BigDecimal totalOutputGst = BigDecimal.ZERO;
        BigDecimal totalInputGst = BigDecimal.ZERO;
        List<GstSlabSummaryDTO> slabList = new ArrayList<>();

        // Sort slabs by rate
        List<BigDecimal> sortedRates = new ArrayList<>(slabMap.keySet());
        Collections.sort(sortedRates);

        for (BigDecimal rate : sortedRates) {
            GstSlabSummaryDTO dto = slabMap.get(rate);
            dto.setNetPayable(dto.getTotalOutputGst().subtract(dto.getTotalInputGst()));
            slabList.add(dto);

            totalOutputGst = totalOutputGst.add(dto.getTotalOutputGst());
            totalInputGst = totalInputGst.add(dto.getTotalInputGst());
        }

        return GstSummaryDTO.builder()
                .outputGstSales(totalOutputGst)
                .salesInvoiceCount(sales.size())
                .inputGstPurchases(totalInputGst)
                .purchaseOrderCount(purchases.size())
                .netGstPayable(totalOutputGst.subtract(totalInputGst))
                .itcAvailable(totalInputGst)
                .slabs(slabList)
                .build();
    }

    private GstSlabSummaryDTO createEmptySlab(BigDecimal rate) {
        return GstSlabSummaryDTO.builder()
                .gstSlab(rate.stripTrailingZeros().toPlainString() + "%")
                .outputTaxable(BigDecimal.ZERO)
                .cgst(BigDecimal.ZERO)
                .sgst(BigDecimal.ZERO)
                .totalOutputGst(BigDecimal.ZERO)
                .inputTaxable(BigDecimal.ZERO)
                .cgstItc(BigDecimal.ZERO)
                .sgstItc(BigDecimal.ZERO)
                .totalInputGst(BigDecimal.ZERO)
                .netPayable(BigDecimal.ZERO)
                .build();
    }
}
