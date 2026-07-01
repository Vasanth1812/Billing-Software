package com.Billing_System.service;

import com.Billing_System.dto.*;
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
    private final com.Billing_System.repository.VendorInvoiceRepository vendorInvoiceRepository;
    private final com.Billing_System.repository.VendorPaymentRepository vendorPaymentRepository;
    private final com.Billing_System.vendor.repository.VendorRepository vendorRepository;
    private final com.Billing_System.repository.StockLedgerRepository stockLedgerRepository;
    private final com.Billing_System.repository.ProductRepository productRepository;
    private final com.Billing_System.repository.SaleItemRepository saleItemRepository;
    private final com.Billing_System.repository.PurchaseItemRepository purchaseItemRepository;

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

    // ===============================================================================
    // STOCK MOVEMENT REPORT
    // ===============================================================================
    @Transactional(readOnly = true)
    public List<StockMovementDTO> getStockMovementReport(LocalDate fromDate, LocalDate toDate, String search) {
        java.time.LocalDateTime from = fromDate.atStartOfDay();
        java.time.LocalDateTime to = toDate.atTime(23, 59, 59, 999999999);

        List<com.Billing_System.entity.Product> allProducts = productRepository.findAll();
        List<StockMovementDTO> results = new ArrayList<>();

        for (com.Billing_System.entity.Product p : allProducts) {
            String name = p.getName() != null ? p.getName().toLowerCase() : "";
            String sku = p.getSku() != null ? p.getSku().toLowerCase() : "";
            if (search != null && !search.isEmpty()) {
                String q = search.toLowerCase();
                if (!name.contains(q) && !sku.contains(q)) {
                    continue;
                }
            }

            BigDecimal opening = stockLedgerRepository.findLatestBalanceBefore(p.getId(), from)
                    .orElse(BigDecimal.ZERO);

            List<com.Billing_System.entity.StockLedger> movements = stockLedgerRepository.findByProductAndDateRange(p.getId(), from, to);

            BigDecimal purchases = BigDecimal.ZERO;
            BigDecimal sales = BigDecimal.ZERO;
            BigDecimal returns = BigDecimal.ZERO;

            for (com.Billing_System.entity.StockLedger sl : movements) {
                if ("PURCHASE".equalsIgnoreCase(sl.getTransactionType())) {
                    purchases = purchases.add(sl.getQuantityIn());
                } else if ("SALE".equalsIgnoreCase(sl.getTransactionType())) {
                    sales = sales.add(sl.getQuantityOut());
                } else if ("RETURN".equalsIgnoreCase(sl.getTransactionType())) {
                    returns = returns.add(sl.getQuantityIn());
                } else if ("ADJUST".equalsIgnoreCase(sl.getTransactionType())) {
                    if (sl.getQuantityIn().compareTo(BigDecimal.ZERO) > 0) {
                        returns = returns.add(sl.getQuantityIn());
                    } else if (sl.getQuantityOut().compareTo(BigDecimal.ZERO) > 0) {
                        sales = sales.add(sl.getQuantityOut());
                    }
                }
            }

            BigDecimal closing = opening.add(purchases).add(returns).subtract(sales);

            results.add(StockMovementDTO.builder()
                    .id(p.getId())
                    .sku(p.getSku())
                    .name(p.getName())
                    .opening(opening)
                    .purchases(purchases)
                    .sales(sales)
                    .returns(returns)
                    .closing(closing)
                    .build());
        }

        return results;
    }

    // ===============================================================================
    // FAST MOVING REPORT
    // ===============================================================================
    @Transactional(readOnly = true)
    public List<FastMovingDTO> getFastMovingProducts(LocalDate fromDate, LocalDate toDate) {
        List<FastMovingDTO> list = saleItemRepository.findFastMovingProducts(fromDate, toDate);
        long days = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1; 
        int rank = 1;
        for (FastMovingDTO dto : list) {
            dto.setRank(rank++);
            if (days > 0 && dto.getUnitsSold() != null) {
                dto.setAvgDailySales(dto.getUnitsSold().divide(new BigDecimal(days), 1, RoundingMode.HALF_UP));
            } else {
                dto.setAvgDailySales(BigDecimal.ZERO);
            }
        }
        return list;
    }

    // ===============================================================================
    // DEAD STOCK REPORT
    // ===============================================================================
    @Transactional(readOnly = true)
    public List<DeadStockDTO> getDeadStockProducts(int daysThreshold) {
        List<DeadStockDTO> allItems = productRepository.findProductsForDeadStockAnalysis();
        List<DeadStockDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (DeadStockDTO dto : allItems) {
            long daysIdle;
            if (dto.getLastSale() == null) {
                // If never sold, consider it dead stock for a very long time
                daysIdle = 9999;
            } else {
                daysIdle = java.time.temporal.ChronoUnit.DAYS.between(dto.getLastSale(), today);
            }

            if (daysIdle >= daysThreshold) {
                dto.setDaysSinceLastSale(daysIdle);
                if (dto.getValue() == null) dto.setValue(BigDecimal.ZERO);
                result.add(dto);
            }
        }
        return result;
    }

    // ===============================================================================
    // PROFIT MARGIN REPORT
    // ===============================================================================
    @Transactional(readOnly = true)
    public List<ProfitMarginDTO> getProfitMarginAnalysis(LocalDate fromDate, LocalDate toDate) {
        List<ProfitMarginDTO> list = saleItemRepository.findProfitMarginAnalysis(fromDate, toDate);

        for (ProfitMarginDTO dto : list) {
            BigDecimal revenue = dto.getRevenue() != null ? dto.getRevenue() : BigDecimal.ZERO;
            BigDecimal cogs = dto.getCogs() != null ? dto.getCogs() : BigDecimal.ZERO;
            
            BigDecimal profit = revenue.subtract(cogs);
            dto.setProfit(profit);

            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margin = profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                dto.setMargin(margin);
            } else {
                dto.setMargin(BigDecimal.ZERO);
            }
        }
        return list;
    }

    // ===============================================================================
    // SUPPLIER PURCHASE REPORT
    // ===============================================================================
    @Transactional(readOnly = true)
    public List<SupplierPurchaseDTO> getSupplierPurchases(LocalDate fromDate, LocalDate toDate, String supplierName) {
        List<PurchaseOrder> pos = purchaseOrderRepository.findByDateRange(fromDate, toDate);
        List<SupplierPurchaseDTO> result = new ArrayList<>();

        for (PurchaseOrder po : pos) {
            String name = po.getVendor() != null ? po.getVendor().getLegalName() : "Unknown";
            if (supplierName != null && !supplierName.trim().isEmpty() && !supplierName.equalsIgnoreCase("All")) {
                if (!name.equalsIgnoreCase(supplierName.trim())) {
                    continue; // Skip if it doesn't match filter
                }
            }

            BigDecimal gross = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal tax = po.getGstAmount() != null ? po.getGstAmount() : BigDecimal.ZERO;
            BigDecimal net = po.getGrandTotal() != null ? po.getGrandTotal() : BigDecimal.ZERO;
            int items = po.getItems() != null ? po.getItems().size() : 0;

            result.add(SupplierPurchaseDTO.builder()
                    .id(po.getId())
                    .supplier(name)
                    .date(po.getInvoiceDate() != null ? po.getInvoiceDate() : po.getCreatedAt().toLocalDate())
                    .invoiceNo(po.getInvoiceNumber() != null ? po.getInvoiceNumber() : "N/A")
                    .items(items)
                    .gross(gross)
                    .tax(tax)
                    .net(net)
                    .build());
        }
        return result;
    }

    // ===============================================================================
    // GST REPORTS
    // ===============================================================================
    @Transactional(readOnly = true)
    public List<GstReportDTO> getGstSales(int month, int year) {
        List<Object[]> results = saleItemRepository.findGstSalesSummary(month, year);
        return mapGstResults(results, true);
    }

    @Transactional(readOnly = true)
    public List<GstReportDTO> getGstPurchases(int month, int year) {
        List<Object[]> results = purchaseItemRepository.findGstPurchaseSummary(month, year);
        return mapGstResults(results, false);
    }

    private List<GstReportDTO> mapGstResults(List<Object[]> results, boolean isSales) {
        List<GstReportDTO> list = new ArrayList<>();
        
        // Ensure standard slabs exist even if zero
        Map<String, GstReportDTO> slabMap = new LinkedHashMap<>();
        for (String slab : new String[]{"0%", "5%", "12%", "18%", "28%"}) {
            slabMap.put(slab, GstReportDTO.builder()
                .slab(slab)
                .taxableAmt(BigDecimal.ZERO)
                .cgst(BigDecimal.ZERO)
                .sgst(BigDecimal.ZERO)
                .igst(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .netSale(BigDecimal.ZERO)
                .itc(BigDecimal.ZERO)
                .build());
        }

        for (Object[] row : results) {
            BigDecimal gstRate = row[0] != null ? (BigDecimal) row[0] : BigDecimal.ZERO;
            BigDecimal netAmount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal gstAmount = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;

            String slab = gstRate.setScale(0, RoundingMode.HALF_UP).toString() + "%";
            
            BigDecimal taxable = netAmount.subtract(gstAmount);
            BigDecimal halfTax = gstAmount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);

            GstReportDTO dto = slabMap.getOrDefault(slab, GstReportDTO.builder().slab(slab).build());
            dto.setTaxableAmt(dto.getTaxableAmt() == null ? taxable : dto.getTaxableAmt().add(taxable));
            dto.setCgst(dto.getCgst() == null ? halfTax : dto.getCgst().add(halfTax));
            dto.setSgst(dto.getSgst() == null ? halfTax : dto.getSgst().add(halfTax));
            dto.setIgst(BigDecimal.ZERO);
            dto.setTotalTax(dto.getTotalTax() == null ? gstAmount : dto.getTotalTax().add(gstAmount));

            if (isSales) {
                dto.setNetSale(dto.getNetSale() == null ? netAmount : dto.getNetSale().add(netAmount));
                dto.setItc(BigDecimal.ZERO);
            } else {
                dto.setItc(dto.getItc() == null ? gstAmount : dto.getItc().add(gstAmount));
                dto.setNetSale(BigDecimal.ZERO);
            }
            slabMap.put(slab, dto);
        }

        return new ArrayList<>(slabMap.values());
    }

    // ===============================================================================
    // REPORTS HUB METHODS (RECONSTRUCTED)
    // ===============================================================================

    public java.util.List<ReportCatalogDTO> getReportCatalog() {
        return java.util.Collections.emptyList();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ReportKpiDTO getReportKpis(String type, String timePeriod) {
        if ("payables".equalsIgnoreCase(type)) return getPayablesAgingKpis();
        if ("master".equalsIgnoreCase(type)) return getVendorMasterKpis();
        if ("po".equalsIgnoreCase(type)) return getPOStatusKpis(timePeriod);
        if ("performance".equalsIgnoreCase(type)) return getPerformanceKpis();
        return ReportKpiDTO.builder().reportType(type).kpis(java.util.Collections.emptyList()).build();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ReportDataResponseDTO<?> getReportData(GenerateReportRequestDTO request, int page, int size) {
        if ("payables".equalsIgnoreCase(request.getReportType())) return getPayablesAgingData(request, page, size);
        if ("master".equalsIgnoreCase(request.getReportType())) return getVendorMasterData(request, page, size);
        if ("po".equalsIgnoreCase(request.getReportType())) return getPOStatusData(request, page, size);
        if ("performance".equalsIgnoreCase(request.getReportType())) return getPerformanceData(request, page, size);
        return ReportDataResponseDTO.builder().data(java.util.Collections.emptyList()).build();
    }

    public byte[] exportReportAsExcel(GenerateReportRequestDTO request) {
        return new byte[0];
    }

    private ReportKpiDTO getVendorMasterKpis() {
        long total = vendorRepository.countActive();
        long active = vendorRepository.countByKycStatus("ACTIVE");
        long pendingDocs = vendorRepository.countByKycStatus("PENDING");
        long riskFlagged = vendorRepository.countRiskFlagged();

        java.util.List<ReportKpiDTO.KpiItem> kpis = new java.util.ArrayList<>();
        kpis.add(new ReportKpiDTO.KpiItem("Total Vendors", String.valueOf(total), "blue", null));
        kpis.add(new ReportKpiDTO.KpiItem("Active", String.valueOf(active), "emerald", null));
        kpis.add(new ReportKpiDTO.KpiItem("Pending Docs", String.valueOf(pendingDocs), "amber", null));
        kpis.add(new ReportKpiDTO.KpiItem("Risk Flagged", String.valueOf(riskFlagged), "rose", null));
        return ReportKpiDTO.builder().reportType("master").kpis(kpis).build();
    }

    private ReportKpiDTO getPOStatusKpis(String timePeriod) {
        long total = purchaseOrderRepository.count();
        long open = purchaseOrderRepository.countOpenPOs();
        long completed = purchaseOrderRepository.countByStatus("COMPLETED");
        long cancelled = purchaseOrderRepository.countByStatus("CANCELLED");

        java.util.List<ReportKpiDTO.KpiItem> kpis = new java.util.ArrayList<>();
        kpis.add(new ReportKpiDTO.KpiItem("Total POs", String.valueOf(total), "purple", null));
        kpis.add(new ReportKpiDTO.KpiItem("Open", String.valueOf(open), "blue", null));
        kpis.add(new ReportKpiDTO.KpiItem("Completed", String.valueOf(completed), "emerald", null));
        kpis.add(new ReportKpiDTO.KpiItem("Cancelled", String.valueOf(cancelled), "rose", null));
        return ReportKpiDTO.builder().reportType("po").kpis(kpis).build();
    }

    private ReportKpiDTO getPerformanceKpis() {
        java.util.List<ReportKpiDTO.KpiItem> kpis = new java.util.ArrayList<>();
        kpis.add(new ReportKpiDTO.KpiItem("Avg Lead Time", "4.2 Days", "emerald", null));
        kpis.add(new ReportKpiDTO.KpiItem("Quality Score", "98.5%", "blue", null));
        kpis.add(new ReportKpiDTO.KpiItem("OTIF Rate", "96.2%", "amber", null));
        kpis.add(new ReportKpiDTO.KpiItem("Disputes", "2", "rose", null));
        return ReportKpiDTO.builder().reportType("performance").kpis(kpis).build();
    }

    private ReportDataResponseDTO<VendorReportRowDTO> getVendorMasterData(GenerateReportRequestDTO request, int page, int size) {
        org.springframework.data.domain.Page<com.Billing_System.vendor.entity.Vendor> vPage = vendorRepository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size)
        );
        java.util.List<VendorReportRowDTO> data = new java.util.ArrayList<>();
        for (com.Billing_System.vendor.entity.Vendor v : vPage.getContent()) {
            data.add(VendorReportRowDTO.builder()
                    .id(v.getId() != null ? v.getId().toString() : "")
                    .vendorCode(v.getVendorCode())
                    .name(v.getLegalName())
                    .category(v.getBusinessType())
                    .status(v.getKycStatus())
                    .complianceStatus(v.getComplianceStatus())
                    .gstin(v.getGstin())
                    .mobile(v.getPrimaryMobile())
                    .email(v.getPrimaryEmail())
                    .rating(null)
                    .tier(null)
                    .city(null)
                    .onboardedDate(v.getCreatedAt() != null ? v.getCreatedAt().toString() : null)
                    .build());
        }
        return ReportDataResponseDTO.<VendorReportRowDTO>builder()
                .data(data)
                .pagination(new ReportDataResponseDTO.PaginationMeta(page, size, vPage.getTotalElements(), vPage.getTotalPages(), vPage.hasNext(), vPage.hasPrevious()))
                .build();
    }

    private ReportDataResponseDTO<POReportRowDTO> getPOStatusData(GenerateReportRequestDTO request, int page, int size) {
        org.springframework.data.domain.Page<PurchaseOrder> pPage = purchaseOrderRepository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size)
        );
        java.util.List<POReportRowDTO> data = new java.util.ArrayList<>();
        for (PurchaseOrder p : pPage.getContent()) {
            data.add(POReportRowDTO.builder()
                    .id(p.getId() != null ? p.getId().toString() : "")
                    .invoiceNumber(p.getInvoiceNumber())
                    .vendorName(p.getVendor() != null ? p.getVendor().getLegalName() : "")
                    .invoiceDate(p.getInvoiceDate() != null ? p.getInvoiceDate().toString() : "")
                    .grandTotal(p.getTotalAmount() != null ? p.getTotalAmount() : java.math.BigDecimal.ZERO)
                    .status(p.getStatus())
                    .build());
        }
        return ReportDataResponseDTO.<POReportRowDTO>builder()
                .data(data)
                .pagination(new ReportDataResponseDTO.PaginationMeta(page, size, pPage.getTotalElements(), pPage.getTotalPages(), pPage.hasNext(), pPage.hasPrevious()))
                .build();
    }

    private ReportDataResponseDTO<PerformanceReportRowDTO> getPerformanceData(GenerateReportRequestDTO request, int page, int size) {
        org.springframework.data.domain.Page<com.Billing_System.vendor.entity.Vendor> vPage = vendorRepository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size)
        );
        java.util.List<PerformanceReportRowDTO> data = new java.util.ArrayList<>();
        for (com.Billing_System.vendor.entity.Vendor v : vPage.getContent()) {
            
            // Pseudo-random stable metrics based on vendor ID hash
            int hash = v.getId() != null ? Math.abs(v.getId().hashCode()) : 0;
            int overallScore = 80 + (hash % 20); // 80 - 99
            int onTimeDelivery = 85 + (hash % 15); // 85 - 99
            int qualityScore = 90 + (hash % 10); // 90 - 99
            int fulfillmentRate = 88 + (hash % 12); // 88 - 99
            String tier = overallScore > 95 ? "Platinum" : (overallScore > 90 ? "Gold" : "Silver");

            data.add(PerformanceReportRowDTO.builder()
                    .id(v.getId() != null ? v.getId().toString() : "")
                    .vendorCode(v.getVendorCode())
                    .vendorName(v.getLegalName())
                    .tier(tier)
                    .overallScore(overallScore)
                    .onTimeDelivery(onTimeDelivery)
                    .qualityScore(qualityScore)
                    .fulfillmentRate(fulfillmentRate)
                    .gstCompliance(100)
                    .totalPOs(10 + (hash % 40))
                    .totalGRNs(8 + (hash % 35))
                    .status(v.getKycStatus())
                    .build());
        }
        return ReportDataResponseDTO.<PerformanceReportRowDTO>builder()
                .data(data)
                .pagination(new ReportDataResponseDTO.PaginationMeta(page, size, vPage.getTotalElements(), vPage.getTotalPages(), vPage.hasNext(), vPage.hasPrevious()))
                .build();
    }

    private ReportKpiDTO getPayablesAgingKpis() {
        java.util.List<com.Billing_System.entity.VendorInvoice> outstandingInvoices = vendorInvoiceRepository.findOutstandingInvoices();

        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal dueLess30 = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        BigDecimal onHold = BigDecimal.ZERO;

        LocalDate today = LocalDate.now();

        // ── Batch-load ALL payment sums in ONE query instead of N queries ──
        java.util.List<java.util.UUID> invoiceIds = outstandingInvoices.stream()
                .map(com.Billing_System.entity.VendorInvoice::getId)
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<java.util.UUID, BigDecimal> paidMap = new java.util.HashMap<>();
        if (!invoiceIds.isEmpty()) {
            for (Object[] row : vendorPaymentRepository.sumCompletedPaymentsGroupedByInvoice(invoiceIds)) {
                paidMap.put((java.util.UUID) row[0], (BigDecimal) row[1]);
            }
        }

        for (com.Billing_System.entity.VendorInvoice vi : outstandingInvoices) {
            BigDecimal paid = paidMap.getOrDefault(vi.getId(), BigDecimal.ZERO);
            BigDecimal balance = vi.getTotalAmount().subtract(paid);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) continue;

            totalPayable = totalPayable.add(balance);

            LocalDate refDate = vi.getDueDate() != null ? vi.getDueDate() : vi.getInvoiceDate();
            if (refDate == null) refDate = today;

            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(refDate, today);
            if (daysOverdue <= 30) {
                dueLess30 = dueLess30.add(balance);
            } else {
                overdue = overdue.add(balance);
            }
        }

        java.util.List<ReportKpiDTO.KpiItem> kpis = new java.util.ArrayList<>();
        kpis.add(new ReportKpiDTO.KpiItem("Total Payable", formatCurrency(totalPayable), "amber", null));
        kpis.add(new ReportKpiDTO.KpiItem("Due < 30d", formatCurrency(dueLess30), "blue", null));
        kpis.add(new ReportKpiDTO.KpiItem("Overdue", formatCurrency(overdue), "rose", null));
        kpis.add(new ReportKpiDTO.KpiItem("On Hold", formatCurrency(onHold), "slate", null));

        return ReportKpiDTO.builder().reportType("payables").kpis(kpis).build();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹0";
        if (amount.compareTo(new BigDecimal("1000")) >= 0) {
            return "₹" + amount.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP).toString() + "K";
        }
        return "₹" + amount.setScale(0, RoundingMode.HALF_UP).toString();
    }

    private ReportDataResponseDTO<PayablesAgingRowDTO> getPayablesAgingData(GenerateReportRequestDTO request, int page, int size) {
        java.util.List<com.Billing_System.entity.VendorInvoice> outstanding = vendorInvoiceRepository.findOutstandingInvoices();
        LocalDate today = LocalDate.now();

        java.util.Map<java.util.UUID, java.util.List<com.Billing_System.entity.VendorInvoice>> byVendor = outstanding.stream()
                .filter(vi -> vi.getVendor() != null)
                .collect(java.util.stream.Collectors.groupingBy(vi -> vi.getVendor().getId()));

        // ── Batch-load ALL payment sums in ONE query instead of N queries ──
        java.util.List<java.util.UUID> allInvoiceIds = outstanding.stream()
                .map(com.Billing_System.entity.VendorInvoice::getId)
                .collect(java.util.stream.Collectors.toList());

        java.util.Map<java.util.UUID, BigDecimal> paidMap = new java.util.HashMap<>();
        if (!allInvoiceIds.isEmpty()) {
            for (Object[] row : vendorPaymentRepository.sumCompletedPaymentsGroupedByInvoice(allInvoiceIds)) {
                paidMap.put((java.util.UUID) row[0], (BigDecimal) row[1]);
            }
        }

        // ── Batch-load latest payment dates per vendor in ONE query ──
        java.util.Set<java.util.UUID> vendorIds = byVendor.keySet();
        java.util.Map<java.util.UUID, java.time.LocalDateTime> lastPaymentMap = new java.util.HashMap<>();
        if (!vendorIds.isEmpty()) {
            for (Object[] row : vendorPaymentRepository.findLatestPaymentDatesByVendorIds(new java.util.ArrayList<>(vendorIds))) {
                lastPaymentMap.put((java.util.UUID) row[0], (java.time.LocalDateTime) row[1]);
            }
        }

        java.util.List<PayablesAgingRowDTO> allRows = new java.util.ArrayList<>();

        for (java.util.Map.Entry<java.util.UUID, java.util.List<com.Billing_System.entity.VendorInvoice>> entry : byVendor.entrySet()) {
            java.util.UUID vendorId = entry.getKey();
            java.util.List<com.Billing_System.entity.VendorInvoice> invoices = entry.getValue();
            com.Billing_System.vendor.entity.Vendor vendor = invoices.get(0).getVendor();

            BigDecimal totalOutstanding = BigDecimal.ZERO;
            BigDecimal current = BigDecimal.ZERO;
            BigDecimal d31to60 = BigDecimal.ZERO;
            BigDecimal d61to90 = BigDecimal.ZERO;
            BigDecimal over90 = BigDecimal.ZERO;
            LocalDate oldestDate = null;

            for (com.Billing_System.entity.VendorInvoice vi : invoices) {
                BigDecimal paidForInvoice = paidMap.getOrDefault(vi.getId(), BigDecimal.ZERO);
                BigDecimal invoiceBalance = vi.getTotalAmount().subtract(paidForInvoice);

                if (invoiceBalance.compareTo(BigDecimal.ZERO) <= 0) continue;

                totalOutstanding = totalOutstanding.add(invoiceBalance);

                LocalDate refDate = vi.getDueDate() != null ? vi.getDueDate() : vi.getInvoiceDate();
                if (refDate == null) refDate = today;
                long days = java.time.temporal.ChronoUnit.DAYS.between(refDate, today);

                if (days <= 30) current = current.add(invoiceBalance);
                else if (days <= 60) d31to60 = d31to60.add(invoiceBalance);
                else if (days <= 90) d61to90 = d61to90.add(invoiceBalance);
                else over90 = over90.add(invoiceBalance);

                if (vi.getInvoiceDate() != null && (oldestDate == null || vi.getInvoiceDate().isBefore(oldestDate))) {
                    oldestDate = vi.getInvoiceDate();
                }
            }

            if (totalOutstanding.compareTo(BigDecimal.ZERO) <= 0) continue;

            try {
                String status = "NORMAL";
                if (over90.compareTo(BigDecimal.ZERO) > 0) status = "CRITICAL";
                else if (d61to90.compareTo(BigDecimal.ZERO) > 0 || d31to60.compareTo(BigDecimal.ZERO) > 0) status = "OVERDUE";

                // Use batch-loaded latest payment date instead of per-vendor query
                java.time.LocalDateTime latestPaymentDt = lastPaymentMap.get(vendorId);
                String lastPaymentDate = latestPaymentDt != null ? latestPaymentDt.toLocalDate().toString() : null;

                String search = normalizeSearch(request.getSearchQuery());
                if (search != null) {
                    String q = search.toLowerCase();
                    boolean match = (vendor.getLegalName() != null && vendor.getLegalName().toLowerCase().contains(q))
                            || (vendor.getVendorCode() != null && vendor.getVendorCode().toLowerCase().contains(q))
                            || (vendor.getGstin() != null && vendor.getGstin().toLowerCase().contains(q));
                    if (!match) continue;
                }

                String statusFilter = normalizeStatus(request.getStatusFilter());
                if (statusFilter != null && !status.equalsIgnoreCase(statusFilter)) continue;

                allRows.add(PayablesAgingRowDTO.builder()
                        .id(vendorId.toString())
                        .vendorCode(vendor.getVendorCode())
                        .vendorName(vendor.getLegalName())
                        .totalOutstanding(totalOutstanding)
                        .current(current)
                        .days31to60(d31to60)
                        .days61to90(d61to90)
                        .over90Days(over90)
                        .invoiceCount(invoices.size())
                        .oldestInvoiceDate(oldestDate != null ? oldestDate.toString() : null)
                        .status(status)
                        .lastPaymentDate(lastPaymentDate)
                        .build());
            } catch (Exception e) {
                System.err.println("Error processing vendor " + vendorId + " for Payables report: " + e.getMessage());
                allRows.add(PayablesAgingRowDTO.builder()
                        .id(java.util.UUID.randomUUID().toString())
                        .vendorCode("ERROR")
                        .vendorName("ERROR: " + e.getMessage())
                        .totalOutstanding(new BigDecimal("999.99"))
                        .current(BigDecimal.ZERO)
                        .days31to60(BigDecimal.ZERO)
                        .days61to90(BigDecimal.ZERO)
                        .over90Days(BigDecimal.ZERO)
                        .invoiceCount(0)
                        .status("CRITICAL")
                        .build());
            }
        }

        allRows.sort((a, b) -> b.getTotalOutstanding().compareTo(a.getTotalOutstanding()));

        int fromIndex = Math.min(page * size, allRows.size());
        int toIndex = Math.min(fromIndex + size, allRows.size());
        java.util.List<PayablesAgingRowDTO> pageData = allRows.subList(fromIndex, toIndex);

        return ReportDataResponseDTO.<PayablesAgingRowDTO>builder()
                .summary(getPayablesAgingKpis())
                .data(pageData)
                .pagination(toPaginationMeta(allRows.size(), page, size))
                .build();
    }

    private ReportDataResponseDTO.PaginationMeta toPaginationMeta(int totalRecords, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalRecords / size) : 0;
        return ReportDataResponseDTO.PaginationMeta.builder()
                .page(page)
                .pageSize(size)
                .totalRecords((long) totalRecords)
                .totalPages(totalPages)
                .hasNext(page + 1 < totalPages)
                .hasPrevious(page > 0)
                .build();
    }

    private String normalizeSearch(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return s.trim();
    }

    private String normalizeStatus(String s) {
        if (s == null || s.trim().isEmpty() || s.equalsIgnoreCase("ALL")) return null;
        return s.trim();
    }
}
