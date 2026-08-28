package com.sribalaji.erp.config;

import com.sribalaji.erp.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/login").permitAll()

                // ---- ADMIN only: master data delete, sensitive reports, user management ----
                .requestMatchers("/products/delete/**", "/parties/delete/**").hasRole("ADMIN")
                .requestMatchers("/reports/gst-summary", "/reports/outstanding").hasRole("ADMIN")
                .requestMatchers("/users/**").hasRole("ADMIN")

                // ---- Both ADMIN and CASHIER ----
                .requestMatchers("/pos/**", "/api/pos/**").hasAnyRole("ADMIN", "CASHIER")
                .requestMatchers("/stock-adjustment/**", "/api/stock-adjustment/**").hasAnyRole("ADMIN", "CASHIER")
                .requestMatchers("/products/view/**", "/api/products/**").hasAnyRole("ADMIN", "CASHIER")
                .requestMatchers("/reports/stock", "/reports/stock-adjustment-history").hasAnyRole("ADMIN", "CASHIER")
                .requestMatchers("/payments/**").hasAnyRole("ADMIN", "CASHIER")

                // ---- Master data CRUD (create/edit) - both roles can view, only ADMIN can be enforced per-action in controller ----
                .requestMatchers("/products/**", "/parties/**", "/purchase/**").hasAnyRole("ADMIN", "CASHIER")

                .requestMatchers("/dashboard", "/").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(3)
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")); // AJAX endpoints - see note below

        return http.build();
    }
}
