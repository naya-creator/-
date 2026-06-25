package com.EcommerceApp.H2NS.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.EcommerceApp.H2NS.model.Invoice;
import com.EcommerceApp.H2NS.model.Order;
import com.EcommerceApp.H2NS.repository.InvoiceRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Async("invoiceProcessingExecutor")
    public void generateInvoiceAsync(Order order) {

        log.info("INVOICE THREAD: {} - Generating invoice for order {}",
                Thread.currentThread().getName(),
                order.getId());

        try {

           
            Thread.sleep(2000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        Invoice invoice = new Invoice();

        invoice.setOrder(order);

        invoice.setInvoiceNumber(
                Invoice.generateInvoiceNumber(order.getId())
        );

        invoice.setTotalAmount(order.getTotalAmount());

        invoice.setStatus(Invoice.InvoiceStatus.GENERATED);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        log.info("Invoice generated successfully: {}",
                savedInvoice.getInvoiceNumber());

       
        simulateHeavyInvoiceTasksAsync(savedInvoice);
    }

    @Async("invoiceProcessingExecutor")
    public void simulateHeavyInvoiceTasksAsync(Invoice invoice) {

        log.info("BACKGROUND THREAD: {} - Processing PDF/Email for invoice {}",
                Thread.currentThread().getName(),
                invoice.getInvoiceNumber());

        try {

            Thread.sleep(3000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }

        log.info("Heavy background tasks completed for invoice {}",
                invoice.getInvoiceNumber());
    }

    public Invoice getInvoiceByOrderId(Long orderId) {

        return invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invoice not found for order id: " + orderId
                        )
                );
    }
}