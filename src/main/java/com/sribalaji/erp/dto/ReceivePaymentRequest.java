package com.sribalaji.erp.dto;

import com.sribalaji.erp.entity.Payment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ReceivePaymentRequest {

    @NotNull
    private Long customerId;

    @NotNull
    private Long invoiceId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private Payment.PaymentMode paymentMode = Payment.PaymentMode.CASH;

    private String remarks;
}
