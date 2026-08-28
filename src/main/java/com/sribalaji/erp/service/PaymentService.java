package com.sribalaji.erp.service;

import com.sribalaji.erp.dto.ReceivePaymentRequest;
import com.sribalaji.erp.entity.Invoice;
import com.sribalaji.erp.entity.Party;
import com.sribalaji.erp.entity.Payment;
import com.sribalaji.erp.exception.BusinessException;
import com.sribalaji.erp.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PartyService partyService;
    private final InvoiceService invoiceService;

    public List<Payment> findByCustomer(Party customer) {
        return paymentRepository.findByCustomerOrderByPaymentDateDesc(customer);
    }

    /**
     * Records a payment against ONE specific invoice, reduces the invoice's balance due,
     * updates its payment status, and reduces the customer's overall outstanding balance.
     * No interest and no credit-limit checks are applied anywhere, per business requirements.
     */
    public Payment receivePayment(ReceivePaymentRequest request) {
        Party customer = partyService.findById(request.getCustomerId());
        if (customer.getPartyType() != Party.PartyType.CUSTOMER) {
            throw new BusinessException("Selected party is not a customer.");
        }

        Invoice invoice = invoiceService.findById(request.getInvoiceId());
        if (invoice.getCustomer() == null || !invoice.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException("This invoice does not belong to the selected customer.");
        }

        BigDecimal balanceDue = invoice.getBalanceDue();
        if (request.getAmount().compareTo(balanceDue) > 0) {
            throw new BusinessException(String.format(
                    "Payment amount (%s) exceeds the balance due on this invoice (%s).",
                    request.getAmount(), balanceDue));
        }

        Payment payment = new Payment();
        payment.setCustomer(customer);
        payment.setInvoice(invoice);
        payment.setAmount(request.getAmount());
        payment.setPaymentMode(request.getPaymentMode());
        payment.setRemarks(request.getRemarks());
        paymentRepository.save(payment);

        invoiceService.applyPayment(invoice, request.getAmount());
        partyService.adjustOutstandingBalance(customer.getId(), request.getAmount().negate());

        return payment;
    }
}
