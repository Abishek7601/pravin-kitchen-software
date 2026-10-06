package com.example.pravin_quotation.config;

import com.example.pravin_quotation.security.CustomAuthenticationSuccessHandler;
import com.example.pravin_quotation.security.EmployeeOtpFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final EmployeeOtpFilter employeeOtpFilter;

    public SecurityConfig(
            CustomAuthenticationSuccessHandler authenticationSuccessHandler,
            EmployeeOtpFilter employeeOtpFilter
    ) {
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.employeeOtpFilter = employeeOtpFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // Public
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Employee OTP
                        .requestMatchers(
                                "/employee/otp",
                                "/employee/otp/send",
                                "/employee/otp/verify"
                        ).hasRole("EMPLOYEE")

                        // Admin
                        .requestMatchers("/admin/**")
                        .hasRole("SUPER_ADMIN")

                        // Employee
                        .requestMatchers("/employee/**")
                        .hasRole("EMPLOYEE")

                        .anyRequest()
                        .authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .addFilterBefore(
                        employeeOtpFilter,
                        org.springframework.security.web.access.intercept.AuthorizationFilter.class
                );

        return http.build();
    }
}