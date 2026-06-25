package com.EcommerceApp.H2NS.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.EcommerceApp.H2NS.model.Product;
import com.EcommerceApp.H2NS.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product addProduct(String name, String description, BigDecimal price, Integer stockQuantity) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setActive(true);

        Product savedProduct = productRepository.save(product);
        log.info(" Product added successfully: {} (Stock: {})", name, stockQuantity);
        return savedProduct;
    }

    public List<Product> getAllProducts() {
        return productRepository.findByActiveTrue();
    }

    @Cacheable(value = "products", key = "#productId")
    public Product getProductById(Long productId) {
        log.info("Fetching product from DATABASE (Cache Miss): {}", productId);
    
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException(" Product Not Fount with ID : " + productId));
    }

    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findByStockQuantityLessThanEqual(threshold);
    }
}
