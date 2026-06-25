package com.EcommerceApp.H2NS.service;

import org.springframework.scheduling.annotation.Async; // استيراد المكتبة
import org.springframework.stereotype.Service;
import com.EcommerceApp.H2NS.model.Order;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

    
    @Async("notificationExecutor") 
    public void sendOrderConfirmation(Order order) {
        log.info("NOTIFICATION THREAD: {} - Sending order confirmation notification for order ID {}: (Asynchronous)", 
                Thread.currentThread().getName(), order.getId());
     
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Order confirmation notification sent successfully for order ID {}", order.getId());
    }

    @Async("notificationExecutor")
    public void sendNotification(String email, String message) {
        log.info("NOTIFICATION THREAD: {} - Sending notification to {}", 
                Thread.currentThread().getName(), email);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Notification sent to {} successfully", email);
    }
}