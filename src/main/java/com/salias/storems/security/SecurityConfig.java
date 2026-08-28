package com.salias.storems.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Form login backed by {@code Employee} rows.
 *
 * <p>There are no hardcoded credentials and no default passwords: every login is
 * an employee with a BCrypt hash in the database. CSRF protection stays enabled
 * because the UI is server-rendered and Thymeleaf injects the token into forms
 * automatically.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Manager-only areas. Employees get 403 here. */
    private static final String[] MANAGER_PATHS = {
            "/dashboard/**", "/inventory/**", "/products/**",
            "/employees/**", "/sales/**", "/reports/**"
    };

    /** Till and own-sales areas. Managers hold ROLE_EMPLOYEE too, so they can sell. */
    private static final String[] TILL_PATHS = {
            "/pos/**", "/my-sales/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthenticationSuccessHandler successHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(MANAGER_PATHS).hasRole(Roles.STORE_MANAGER)
                        .requestMatchers(TILL_PATHS).hasRole(Roles.EMPLOYEE)
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?loggedOut")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true));

        return http.build();
    }

    /** Managers land on their dashboard, staff go straight to the till. */
    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isManager = authentication.getAuthorities().stream()
                    .anyMatch(a -> Roles.ROLE_STORE_MANAGER.equals(a.getAuthority()));
            response.sendRedirect(request.getContextPath() + (isManager ? "/dashboard" : "/pos"));
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
