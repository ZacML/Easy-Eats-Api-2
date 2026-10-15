package com.easyeats.api.config;

import com.easyeats.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class UserDetailsConfig {

    private final UsuarioRepository usuarioRepository;
    @Bean
    public UserDetailsService userDetailsService() {

        return username -> usuarioRepository
                .findByUsername(username)
                .map(usuario -> {

                    List<SimpleGrantedAuthority> authorities =
                            usuario.getRoles()
                                    .stream()
                                    .map(role ->
                                            new SimpleGrantedAuthority(
                                                    "ROLE_" + role.getNome()
                                            )
                                    )
                                    .toList();

                    return new User(
                            usuario.getUsername(),
                            usuario.getPassword(),
                            authorities
                    );
                })
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuário não encontrado"
                        )
                );
    }
}