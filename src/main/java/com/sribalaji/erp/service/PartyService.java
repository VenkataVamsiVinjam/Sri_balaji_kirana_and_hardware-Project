package com.sribalaji.erp.service;

import com.sribalaji.erp.entity.Party;
import com.sribalaji.erp.exception.ResourceNotFoundException;
import com.sribalaji.erp.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PartyService {

    private final PartyRepository partyRepository;

    public List<Party> findCustomers() {
        return partyRepository.findByPartyTypeAndActiveTrueOrderByNameAsc(Party.PartyType.CUSTOMER);
    }

    public List<Party> findSuppliers() {
        return partyRepository.findByPartyTypeAndActiveTrueOrderByNameAsc(Party.PartyType.SUPPLIER);
    }

    public List<Party> searchCustomers(String term) {
        if (term == null || term.isBlank()) return findCustomers();
        return partyRepository.searchByTypeAndName(Party.PartyType.CUSTOMER, term.trim());
    }

    public List<Party> searchSuppliers(String term) {
        if (term == null || term.isBlank()) return findSuppliers();
        return partyRepository.searchByTypeAndName(Party.PartyType.SUPPLIER, term.trim());
    }

    public Party findById(Long id) {
        return partyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found: id=" + id));
    }

    public List<Party> findCustomersWithDues() {
        return partyRepository.findCustomersWithOutstandingBalance();
    }

    public BigDecimal totalOutstandingDues() {
        BigDecimal total = partyRepository.sumTotalOutstandingDues();
        return total != null ? total : BigDecimal.ZERO;
    }

    public Party save(Party party) {
        if (party.getId() == null) {
            // new party: outstanding balance starts equal to opening balance
            party.setOutstandingBalance(party.getOpeningBalance() != null ? party.getOpeningBalance() : BigDecimal.ZERO);
        }
        return partyRepository.save(party);
    }

    public Party update(Long id, Party incoming) {
        Party existing = findById(id);
        existing.setName(incoming.getName());
        existing.setPhone(incoming.getPhone());
        existing.setEmail(incoming.getEmail());
        existing.setGstin(incoming.getGstin());
        existing.setAddress(incoming.getAddress());
        // openingBalance and outstandingBalance are NOT editable after creation via this method;
        // outstanding balance only changes through invoices, purchases, and payments.
        return partyRepository.save(existing);
    }

    public void softDelete(Long id) {
        Party party = findById(id);
        party.setActive(false);
        partyRepository.save(party);
    }

    /** Adds (positive) or subtracts (negative) delta to/from a party's outstanding balance. No interest, no limit checks - by design. */
    public void adjustOutstandingBalance(Long partyId, BigDecimal delta) {
        Party party = findById(partyId);
        party.setOutstandingBalance(party.getOutstandingBalance().add(delta));
        partyRepository.save(party);
    }
}
