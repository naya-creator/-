package com.EcommerceApp.H2NS.batch;

import com.EcommerceApp.H2NS.model.DailySalesReport;
import com.EcommerceApp.H2NS.model.Order;
import com.EcommerceApp.H2NS.model.OrderItem;
import com.EcommerceApp.H2NS.repository.DailySalesReportRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
public class SalesReportWriter implements ItemWriter<Order> {

    private final DailySalesReportRepository repository;

    public SalesReportWriter(DailySalesReportRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends Order> chunk) throws Exception {
        LocalDate today = LocalDate.now();

        
        DailySalesReport report = repository.findByReportDate(today)
                .orElseGet(() -> {
                    DailySalesReport newReport = new DailySalesReport();
                    newReport.setReportDate(today);
                    newReport.setTotalOrders(0);
                    newReport.setTotalSales(BigDecimal.ZERO);
                    newReport.setTotalItemsSold(0);
                    newReport.setTopSellingProductQuantity(0);
                    return newReport;
                });

        BigDecimal chunkSales = BigDecimal.ZERO;
        int chunkItemsSold = 0;
        Map<String, Integer> productQuantities = new HashMap<>();

       
        for (Order order : chunk) {
            chunkSales = chunkSales.add(order.getTotalAmount());
            
            for (OrderItem item : order.getItems()) {
                int qty = item.getQuantity();
                chunkItemsSold += qty;
                
                String productName = item.getProduct().getName();
                productQuantities.put(productName, productQuantities.getOrDefault(productName, 0) + qty);
            }
        }

        
        report.setTotalOrders(report.getTotalOrders() + chunk.size());
        report.setTotalSales(report.getTotalSales().add(chunkSales));
        report.setTotalItemsSold(report.getTotalItemsSold() + chunkItemsSold);

        
        String currentTopProduct = report.getTopSellingProduct();
        int currentTopQty = report.getTopSellingProductQuantity();

        for (Map.Entry<String, Integer> entry : productQuantities.entrySet()) {
            if (entry.getValue() > currentTopQty) {
                currentTopProduct = entry.getKey();
                currentTopQty = entry.getValue();
            }
        }

        report.setTopSellingProduct(currentTopProduct);
        report.setTopSellingProductQuantity(currentTopQty);

        
        repository.save(report);
    }
}