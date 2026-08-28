package com.sribalaji.erp.repository;

import com.sribalaji.erp.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findByCategoryAndActiveTrueOrderByNameAsc(Product.Category category);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(p.hsnCode) LIKE LOWER(CONCAT('%', :term, '%'))) " +
           "ORDER BY p.name ASC")
    List<Product> searchActiveByNameOrHsn(@Param("term") String term);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.currentStock <= p.reorderLevel")
    List<Product> findLowStockProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.currentStock <= p.reorderLevel")
    long countLowStockProducts();

    long countByActiveTrue();

    boolean existsByHsnCodeAndActiveTrue(String hsnCode);
}
