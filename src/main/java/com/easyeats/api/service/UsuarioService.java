package com.easyeats.api.service;

import com.easyeats.api.dto.UsuarioDTO;
import com.easyeats.api.entity.Role;
import com.easyeats.api.entity.Usuario;
import com.easyeats.api.repository.RoleRepository;
import com.easyeats.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public Usuario salvar(UsuarioDTO dto) {
        Usuario user = new Usuario();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        Set<Role> roles = dto.getRoles().stream()
                .map(nome -> roleRepository.findByNome(nome).orElseThrow())
                .collect(Collectors.toSet());
        user.setRoles(roles);
        return usuarioRepository.save(user);
    }

    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll().stream().map(usuario -> {
            UsuarioDTO dto = new UsuarioDTO();
            dto.setUsername(usuario.getUsername());
            dto.setRoles(usuario.getRoles().stream().map(Role::getNome).collect(Collectors.toSet()));
            return dto;
        }).toList();
    }

    public Optional<Usuario> listarPorNome(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Usuario login(String username, String password) {

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Usuário não informado"
            );
        }

        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Senha não informada"
            );
        }

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário ou senha incorreta"
                ));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário ou senha incorreta"
            );
        }

        return usuario;
    }

}
