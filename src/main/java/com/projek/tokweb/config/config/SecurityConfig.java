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
                                                .requestMatchers(
                                                                "/auth/**",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/assets/**",
                                                                "/assets/img/**",
                                                                "/favicon.ico",
                                                                "/error/**",
                                                                "/admin/**",
                                                                "/user/**",
                                                                "/gold-price/**",
                                                                "/api/cart/**")
                                                .permitAll()
                                                .requestMatchers(
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                // .requestMatchers("/user/**").permitAll()
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
