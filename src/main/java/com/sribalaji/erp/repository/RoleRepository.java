package com.sribalaji.erp.repository;

import com.sribalaji.erp.entity.Role;
import com.sribalaji.erp.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
