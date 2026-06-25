package com.EcommerceApp.H2NS.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.EcommerceApp.H2NS.model.OrderItem;
// import com.EcommerceApp.H2NS.model.Product;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi.product, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi " +
            "GROUP BY oi.product " +
            "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);

    default List<Object[]> findTopSellingProducts(int limit) {
        return findTopSellingProducts(PageRequest.of(0, limit));
    }
}

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import com.EcommerceApp.H2NS.model.OrderItem;

// @Repository
// public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
// }