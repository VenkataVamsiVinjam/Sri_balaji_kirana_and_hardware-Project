package com.sribalaji.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Single table for both CUSTOMER and SUPPLIER, distinguished by `partyType`.
 * outstandingBalance semantics:
 *   - For CUSTOMER: positive = customer owes the shop money (Udhaar/credit)
 *   - For SUPPLIER: positive = shop owes the supplier money
 * No interest, no hard credit limit is applied anywhere in the system (by design).
 */
@Entity
@Table(name = "parties", indexes = {
        @Index(name = "idx_party_name", columnList = "name"),
        @Index(name = "idx_party_type", columnList = "partyType")
})
@Getter
@Setter
public class Party extends Auditable {

    public enum PartyType { CUSTOMER, SUPPLIER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PartyType partyType;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 15)
    private String phone;

    @Column(length = 100)
    private String email;

    /** Optional - GST Identification Number, 15 chars if present */
    @Column(length = 15)
    private String gstin;

    @Column(length = 300)
    private String address;

    @NotNull
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    /** Running balance, updated on every invoice / payment / purchase. Never has interest applied. */
    @NotNull
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;
}
