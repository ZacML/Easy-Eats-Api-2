package com.easyeats.api.config;

import com.easyeats.api.security.JwtAuthFilter;
import com.easyeats.api.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class JwtFilterConfig {

    @Bean
    public JwtAuthFilter jwtAuthFilter(
            UserDetailsService userDetailsService,
            JwtUtil jwtUtil
    ) {
        return new JwtAuthFilter(
                userDetailsService,
                jwtUtil
        );
    }
}