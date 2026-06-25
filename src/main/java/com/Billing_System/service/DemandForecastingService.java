package com.Billing_System.service;

import com.Billing_System.dto.ChartDataDTO;
import com.Billing_System.dto.DemandForecastResponseDTO;
import com.Billing_System.dto.KpiDataDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DemandForecastingService {

    public DemandForecastResponseDTO generateForecast(String storeId, String categoryId, String sku, String dateRange, int leadTime, double safetyMultiplier, boolean includeSeasonality) {
        
        // Mocking historical data calculation for the prototype
        // In a real scenario, this would query SalesInvoice and WarehouseStock
        
        List<ChartDataDTO> chartData = new ArrayList<>();
        Random random = new Random(sku.hashCode() + storeId.hashCode()); // deterministic random based on inputs

        int weeks = getWeeksFromDateRange(dateRange);
        
        int baseDemand = 100 + random.nextInt(200);
        
        for (int i = 0; i < weeks; i++) {
            int actual = (int) (baseDemand + (random.nextGaussian() * 20));
            int forecast = actual + (int) (random.nextGaussian() * 10);
            if (includeSeasonality) {
                // adding simple seasonality multiplier
                actual = (int) (actual * (1 + 0.2 * Math.sin(i)));
                forecast = (int) (forecast * (1 + 0.2 * Math.sin(i)));
            }
            chartData.add(ChartDataDTO.builder()
                    .name("Week " + (i + 1))
                    .sales(actual)
                    .forecast(forecast)
                    .build());
        }

        // Calculate KPIs
        int avgDailySales = baseDemand / 7;
        int maxDailySales = (int) (avgDailySales * 1.5);
        int maxLeadTime = leadTime + 2;

        int safetyStock = (int) (((maxDailySales * maxLeadTime) - (avgDailySales * leadTime)) * safetyMultiplier);
        double daysOfCover = 14.0 + (random.nextDouble() * 10); // Mock current stock cover
        double forecastAccuracy = 90.0 + (random.nextDouble() * 8);

        KpiDataDTO kpiData = KpiDataDTO.builder()
                .daysOfCover(Math.round(daysOfCover * 10.0) / 10.0)
                .safetyStock(safetyStock)
                .forecastAccuracy(Math.round(forecastAccuracy * 10.0) / 10.0)
                .build();

        return DemandForecastResponseDTO.builder()
                .kpiData(kpiData)
                .chartData(chartData)
                .build();
    }

    private int getWeeksFromDateRange(String dateRange) {
        if (dateRange == null) return 12;
        if (dateRange.contains("4")) return 4;
        if (dateRange.contains("26")) return 26;
        if (dateRange.contains("52")) return 52;
        return 12;
    }
}
