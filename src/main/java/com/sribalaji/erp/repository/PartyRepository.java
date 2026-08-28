package com.sribalaji.erp.repository;

import com.sribalaji.erp.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PartyRepository extends JpaRepository<Party, Long> {

    List<Party> findByPartyTypeAndActiveTrueOrderByNameAsc(Party.PartyType partyType);

    @Query("SELECT p FROM Party p WHERE p.active = true AND p.partyType = :type AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY p.name ASC")
    List<Party> searchByTypeAndName(@Param("type") Party.PartyType type, @Param("term") String term);

    @Query("SELECT p FROM Party p WHERE p.partyType = 'CUSTOMER' AND p.outstandingBalance > 0 " +
           "ORDER BY p.outstandingBalance DESC")
    List<Party> findCustomersWithOutstandingBalance();

    @Query("SELECT COALESCE(SUM(p.outstandingBalance), 0) FROM Party p WHERE p.partyType = 'CUSTOMER'")
    BigDecimal sumTotalOutstandingDues();
}
