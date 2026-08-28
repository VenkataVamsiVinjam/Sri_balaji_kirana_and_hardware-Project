package com.sribalaji.erp.repository;

import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.entity.Party;
import com.sribalaji.erp.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCustomerOrderByPaymentDateDesc(Party customer);
    List<Payment> findByInvoiceOrderByPaymentDateDesc(Invoice invoice);
}
