package com.EcommerceApp.H2NS.service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.EcommerceApp.H2NS.model.Cart;
import com.EcommerceApp.H2NS.model.CartItem;
import com.EcommerceApp.H2NS.model.Order;
import com.EcommerceApp.H2NS.model.OrderItem;
import com.EcommerceApp.H2NS.model.Product;
import com.EcommerceApp.H2NS.model.User;
import com.EcommerceApp.H2NS.repository.CartRepository;
import com.EcommerceApp.H2NS.repository.OrderRepository;
import com.EcommerceApp.H2NS.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final UserService userService;
    private final InvoiceService invoiceService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CartRepository cartRepository,
                        UserService userService,
                        InvoiceService invoiceService) {

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.userService = userService;
        this.invoiceService = invoiceService;
    }

    @Async("orderProcessingExecutor")
    @Transactional
    public CompletableFuture<Void> placeOrderAsync(Long userId) {

        log.info("ASYNC ORDER THREAD: {} - Processing order for user {}",
                Thread.currentThread().getName(),
                userId);

        try {

            placeOrderSync(userId);

        } catch (Exception e) {

            log.error("Order processing failed for user {}", userId, e);

        }

        return CompletableFuture.completedFuture(null);
    }

    private Order placeOrderSync(Long userId) {

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {

            try {

                return doPlaceOrder(userId);

            } catch (OptimisticLockingFailureException e) {

                attempt++;

                log.warn("Retry {}/{} for user {}", attempt, maxRetries, userId);
            }
        }

        throw new RuntimeException("Order failed after retries");
    }

    private Order doPlaceOrder(Long userId) {

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart empty");
        }

        BigDecimal totalAmount = cart.getTotalPrice();

        User user = userService.getUserById(userId);

        if (user.getBalance().compareTo(totalAmount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        Order order = new Order();

        order.setUser(cart.getUser());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);

        for (CartItem cartItem : cart.getItems()) {

            Product product = cartItem.getProduct();

            int updatedRows = productRepository.decrementStock(
                    product.getId(),
                    cartItem.getQuantity()
            );

            if (updatedRows == 0) {
                throw new OptimisticLockingFailureException("Stock error");
            }

            Product refreshed = productRepository.findById(product.getId())
                    .orElseThrow();

            OrderItem item = new OrderItem();

            item.setProduct(refreshed);
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtPurchase(refreshed.getPrice());
            item.setOrder(order);

            order.getItems().add(item);
        }

        userService.deductBalance(userId, totalAmount);

        order.setStatus(Order.OrderStatus.CONFIRMED);

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();

        cartRepository.save(cart);

        invoiceService.generateInvoiceAsync(savedOrder);

        log.info("ORDER COMPLETED by THREAD: {}",
                Thread.currentThread().getName());

        return savedOrder;
    }

    public java.util.List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }
}