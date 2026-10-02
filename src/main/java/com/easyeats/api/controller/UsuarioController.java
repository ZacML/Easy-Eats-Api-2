package com.easyeats.api.controller;

import com.easyeats.api.dto.UsuarioDTO;
import com.easyeats.api.entity.Usuario;
import com.easyeats.api.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioService service;

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody UsuarioDTO dto) {
        service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public List<UsuarioDTO> listar() {
        return service.listar();
    }

    @GetMapping("/listar-por-nome/{username}")
    public Optional<Usuario> listarPorNome(@PathVariable String username) {
        return service.listarPorNome(username);
    }

    @PostMapping("/login")
    public Usuario login(
            @RequestParam String username,
            @RequestParam String password
    ) {
        return service.login(username, password);
    }


}