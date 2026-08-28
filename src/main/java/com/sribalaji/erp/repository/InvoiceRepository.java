package com.sribalaji.erp.repository;

import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findTopByOrderByInvoiceNumberDesc();

    List<Invoice> findByInvoiceDateBetweenOrderByInvoiceDateDesc(LocalDateTime start, LocalDateTime end);

    List<Invoice> findByCustomerAndPaymentStatusInOrderByInvoiceDateAsc(
            Party customer, List<Invoice.PaymentStatus> statuses);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end")
    BigDecimal sumSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :start AND :end ORDER BY i.invoiceDate ASC")
    List<Invoice> findForGstReport(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByInvoiceDateBetween(LocalDateTime start, LocalDateTime end);
}
