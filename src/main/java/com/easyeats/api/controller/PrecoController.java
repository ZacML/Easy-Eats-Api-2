package com.easyeats.api.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.easyeats.api.dto.PrecoDto;
import com.easyeats.api.service.PrecoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/preco")
@RequiredArgsConstructor
public class PrecoController {

    private final PrecoService service;

    @PostMapping
    public ResponseEntity<PrecoDto> criar(@Valid @RequestBody PrecoDto preco) {
        PrecoDto salvo = service.salvar(preco);
        if (salvo == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<PrecoDto>> listarTodos() {
        List<PrecoDto> precos = service.listarTodos();
        if (precos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(precos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrecoDto> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<List<PrecoDto>> listarPorProduto(@PathVariable Long produtoId) {
        List<PrecoDto> precos = service.listarPorProduto(produtoId);
        if (precos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(precos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrecoDto> alterar( @Valid @PathVariable Long id, @RequestBody PrecoDto preco) {
        PrecoDto atualizado = service.alterar(id, preco);
        if (atualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}