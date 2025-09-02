package com.projek.tokweb.config.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.projek.tokweb.security.CustomAccessDeniedHandler;
import com.projek.tokweb.security.JwtFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
        @Autowired
        JwtFilter jwtFilter;

        private final CustomAccessDeniedHandler customAccessDeniedHandler;

        public SecurityConfig(CustomAccessDeniedHandler customAccessDeniedHandler) {
                this.customAccessDeniedHandler = customAccessDeniedHandler;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints - no authentication required (order matters!)
                                                .requestMatchers(
                                                                "/auth/**",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/assets/**",
                                                                "/assets/img/**",
                                                                "/favicon.ico",
                                                                "/error/**",
                                                                "/gold-price/**",
                                                                "/validasi/**") // Allow all public endpoints
                                                .permitAll()
                                                // // Specific user endpoints that should be public
                                                // .requestMatchers(
                                                // "/user/products/**", // Allow public access to product endpoints
                                                // "/user/current/**", // Allow public access to current user endpoint
                                                // "/user/home", // Allow public access to user home page
                                                // "/user/katalog") // Allow public access to user catalog page
                                                // .permitAll()
                                                // Swagger documentation endpoints
                                                .requestMatchers(
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                // Admin endpoints - require ADMIN role
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                // Remaining user endpoints - require USER role
                                                .requestMatchers("/user/**").hasRole("USER")
                                                // Any other request requires authentication
                                                .anyRequest().authenticated())
                                .csrf(csrf -> csrf.disable())
                                .httpBasic(httpBasic -> httpBasic.disable())
                                .exceptionHandling(ex -> ex
                                                .accessDeniedHandler(customAccessDeniedHandler)
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        System.out.println("User Belum Login");
                                                        if (!request.getRequestURI().equals("/error/403")) {
                                                                response.sendRedirect("/error/403");
                                                        }
                                                }))
                                .logout(logout -> logout.permitAll());
                http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
