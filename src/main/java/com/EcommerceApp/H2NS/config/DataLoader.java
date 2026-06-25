package com.EcommerceApp.H2NS.config;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.EcommerceApp.H2NS.model.Cart;
import com.EcommerceApp.H2NS.model.CartItem;
import com.EcommerceApp.H2NS.model.Product;
import com.EcommerceApp.H2NS.model.User;
import com.EcommerceApp.H2NS.repository.CartRepository;
import com.EcommerceApp.H2NS.repository.ProductRepository;
import com.EcommerceApp.H2NS.repository.UserRepository;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    public DataLoader(UserRepository userRepository,
                      ProductRepository productRepository,
                      CartRepository cartRepository) {

        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // create test user
        User user = new User();

        user.setName("Test User");
        user.setEmail("test@test.com");
        user.setPassword("1234");
        user.setBalance(new BigDecimal("10000"));

        user = userRepository.save(user);

        // create product
        Product product = new Product();

        product.setName("Laptop");
        product.setDescription("Gaming Laptop");
        product.setPrice(new BigDecimal("500"));
        product.setStockQuantity(100);

        product = productRepository.save(product);

        // create cart
        Cart cart = new Cart();

        cart.setUser(user);
        cart.setItems(new ArrayList<>());

        // create cart item
        CartItem item = new CartItem();

        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(1);

        cart.getItems().add(item);

        cartRepository.save(cart);

        System.out.println(" TEST DATA LOADED");
    }
}