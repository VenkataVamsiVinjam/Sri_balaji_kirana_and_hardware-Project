package com.sribalaji.erp.config;

import com.sribalaji.erp.entity.Role;
import com.sribalaji.erp.entity.RoleName;
import com.sribalaji.erp.entity.User;
import com.sribalaji.erp.repository.RoleRepository;
import com.sribalaji.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Runs on every startup. Since the DB starts empty (per requirements), this seeds:
 *   - the two roles: ADMIN, CASHIER
 *   - one default ADMIN login so you can log in on day one
 *
 * IMPORTANT: change the default admin password immediately after first login in production.
 * Default credentials: username=admin / password=Admin@123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));
        Role cashierRole = roleRepository.findByName(RoleName.CASHIER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.CASHIER)));

        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("Shop Administrator");
            admin.setEnabled(true);
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            log.warn("=================================================================");
            log.warn(" Default ADMIN account created -> username: admin | password: Admin@123");
            log.warn(" PLEASE CHANGE THIS PASSWORD IMMEDIATELY AFTER FIRST LOGIN.");
            log.warn("=================================================================");
        }

        if (!userRepository.existsByUsername("cashier")) {
            User cashier = new User();
            cashier.setUsername("cashier");
            cashier.setPassword(passwordEncoder.encode("Cashier@123"));
            cashier.setFullName("Counter Cashier");
            cashier.setEnabled(true);
            cashier.setRoles(Set.of(cashierRole));
            userRepository.save(cashier);
            log.warn(" Default CASHIER account created -> username: cashier | password: Cashier@123");
        }
    }
}
