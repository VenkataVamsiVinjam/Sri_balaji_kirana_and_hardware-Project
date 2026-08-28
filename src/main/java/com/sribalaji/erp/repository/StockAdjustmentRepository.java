package com.sribalaji.erp.repository;

import com.sribalaji.erp.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {
    List<StockAdjustment> findAllByOrderByAdjustmentDateDesc();
    List<StockAdjustment> findByAdjustmentDateBetweenOrderByAdjustmentDateDesc(LocalDateTime start, LocalDateTime end);
    List<StockAdjustment> findByProductIdOrderByAdjustmentDateDesc(Long productId);
}
