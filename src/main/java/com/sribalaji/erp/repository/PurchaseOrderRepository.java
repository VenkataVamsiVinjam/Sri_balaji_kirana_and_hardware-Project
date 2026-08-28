package com.sribalaji.erp.repository;

import com.sribalaji.erp.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByPurchaseDateDesc();
    long count();
}
