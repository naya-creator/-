package com.EcommerceApp.H2NS.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final TopProductsService topProductsService;
    private final RedisTemplate<String, Object> redisTemplate;
    // @Autowired private OrderService self;

    public OrderService(OrderRepository orderRepository,
            ProductRepository productRepository,
            CartRepository cartRepository,
            UserService userService,
            InvoiceService invoiceService,
            TopProductsService topProductsService,
            RedisTemplate<String, Object> redisTemplate) {

        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.userService = userService;
        this.invoiceService = invoiceService;
        this.topProductsService = topProductsService;
        this.redisTemplate = redisTemplate;
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
        // self.doPlaceOrder(userId);
        return CompletableFuture.completedFuture(null);
    }

    private Order placeOrderSync(Long userId) {

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int maxRetries = 5;
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

    // @Transactional
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

            String lockValue = UUID.randomUUID().toString();

            if (!acquireLock(product.getId(), lockValue)) {
                throw new RuntimeException(
                        "Product is currently locked: " + product.getId());
            }

            try {

                int updatedRows = productRepository.decrementStock(
                        product.getId(),
                        cartItem.getQuantity());

                for (int i = 0; i < cartItem.getQuantity(); i++) {
                    topProductsService.recordProductView(product.getId());
                }

                if (updatedRows == 0) {
                    throw new OptimisticLockingFailureException("Stock error");
                }

            } finally {

                releaseLock(product.getId(), lockValue);
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

    private boolean acquireLock(Long productId, String lockValue) {

        String lockKey = "product_lock:" + productId;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(
                        lockKey,
                        lockValue,
                        10,
                        TimeUnit.SECONDS);

        return Boolean.TRUE.equals(success);
    }

    private void releaseLock(Long productId, String lockValue) {

        String lockKey = "product_lock:" + productId;

        Object currentValue = redisTemplate.opsForValue().get(lockKey);

        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }

    public java.util.List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }
}