package com.sribalaji.erp.dto;

import com.sribalaji.erp.entity.Invoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateInvoiceRequest {

    /** Null => Walk-in customer. */
    private Long customerId;

    /** Used only when customerId is null. */
    private String walkInCustomerName;

    private Invoice.DiscountType discountType = Invoice.DiscountType.FLAT;

    private BigDecimal discountValue = BigDecimal.ZERO;

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<InvoiceItemRequest> items;

    /** If the customer wants to pay something immediately at billing time (e.g. part cash payment). */
    private BigDecimal amountPaidNow = BigDecimal.ZERO;

    private boolean emailInvoice = false;
}
