package com.trainbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration class for defining authentication and authorization rules,
 * form login behavior, CSRF policies, and password encoding beans.
 *
 * @author SLIIT Software Engineering Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the main HTTP security filter chain.
     *
     * @param http the {@link HttpSecurity} to modify
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception in case of configuration errors
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/validate/**", "/dashboard/validate/**")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/trains/search",
                    "/departure-board",
                    "/booking/**",
                    "/payment/**",
                    "/api/**",
                    "/notifications/map",
                    "/notifications/live-map",
                    "/validate",
                    "/validate/**",
                    "/dashboard/validate",
                    "/dashboard/validate/**",
                    "/dashboard/boarding-logs",
                    "/dashboard/boarding-logs/**",
                    "/login",
                    "/register",
                    "/forgot-password",
                    "/reset-password",
                    "/reset-password/**",
                    "/rebooking/**",
                    "/notifications/passenger",
                    "/notifications/passenger/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/error",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/trains/**", "/schedules/**", "/admin/**", "/notifications/status/**").hasAnyRole("COORDINATOR", "ADMIN", "STATION_STAFF")
                .requestMatchers("/dashboard", "/dashboard/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    /**
     * Declares the BCrypt password encoder bean.
     *
     * @return {@link PasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
