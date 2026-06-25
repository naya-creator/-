package com.EcommerceApp.H2NS.service;

import com.EcommerceApp.H2NS.model.Product;
import com.EcommerceApp.H2NS.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TopProductsService {

    private static final String TOP_PRODUCTS_ZSET_KEY = "top:products:views";
    private static final String PRODUCT_VIEW_PREFIX = "product:view:";

    private final CacheService cacheService;
    private final ProductRepository productRepository;

    public TopProductsService(CacheService cacheService,
            ProductRepository productRepository) {
        this.cacheService = cacheService;
        this.productRepository = productRepository;
    }

    public void recordProductView(Long productId) {
        String viewKey = PRODUCT_VIEW_PREFIX + productId;

        Long currentViews = cacheService.incrementCounter(viewKey);

        double score = currentViews.doubleValue();
        cacheService.addToSortedSet(TOP_PRODUCTS_ZSET_KEY, productId, score);

        if (currentViews == 1) {
            cacheService.put(viewKey, 1, 24, TimeUnit.HOURS);
        }
    }

    @Cacheable(value = "topProducts", key = "#limit")
    public List<Product> getTopProductsByViews(int limit) {
        // Get top product IDs from Redis sorted set
        Set<Object> topProductIds = cacheService.getTopFromSortedSet(TOP_PRODUCTS_ZSET_KEY, 0, limit - 1);

        if (topProductIds == null || topProductIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Product> products = new ArrayList<>();
        for (Object idObj : topProductIds) {
            Long productId = ((Number) idObj).longValue();
            productRepository.findById(productId).ifPresent(products::add);
        }

        return products;
    }

    @Cacheable(value = "products", key = "#productId")
    public Product getProductWithCache(Long productId) {
        recordProductView(productId);

        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
    }

    @CacheEvict(value = "products", key = "#productId")
    public void evictProductCache(Long productId) {
        cacheService.evictPattern("topProducts*");
        cacheService.evictPattern("topSellingProducts*");
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupOldCacheData() {
        System.out.println("🧹 Cleaning up old cache data...");
    }
}