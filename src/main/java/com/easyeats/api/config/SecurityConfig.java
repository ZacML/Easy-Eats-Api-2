package com.easyeats.api.config;

import com.easyeats.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sec/publico/**","/swagger-ui/index.html/**").permitAll()
                        .requestMatchers("/usuarios/**", "/error").permitAll()
                        .requestMatchers("/sec/admin/**").hasRole("ADMIN")
                        .requestMatchers("/sec/user/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )

                .httpBasic(httpBasic -> {
                });

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
                .map(usuario -> {

                    List<GrantedAuthority> authorities = usuario.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority(
                                    "ROLE_" + role.getNome()
                            ))
                            .collect(Collectors.toList());

                    return new org.springframework.security.core.userdetails.User(
                            usuario.getUsername(),
                            usuario.getPassword(),
                            authorities
                    );
                })
                .orElseThrow(() ->
                        new UsernameNotFoundException("Usuário não encontrado")
                );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
