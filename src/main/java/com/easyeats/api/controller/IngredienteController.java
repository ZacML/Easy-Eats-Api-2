package com.easyeats.api.controller;

import com.easyeats.api.dto.IngredienteDto;
import com.easyeats.api.service.IngredienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredientes")
@RequiredArgsConstructor
public class IngredienteController {

    private final IngredienteService ingredienteService;

    @PostMapping
    public ResponseEntity<IngredienteDto> criar(@Valid @RequestBody IngredienteDto ingrediente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredienteService.salvar(ingrediente));
    }

    @GetMapping
    public ResponseEntity<List<IngredienteDto>> listarTodos() {
        return ResponseEntity.ok(ingredienteService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredienteDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ingredienteService.buscarPorId(id));
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<IngredienteDto> buscarPorNome(@PathVariable String nome) {
        return ingredienteService.buscarPorNome(nome)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredienteDto> alterar(@PathVariable Long id,
                                                  @Valid @RequestBody IngredienteDto ingrediente) {
        return ResponseEntity.ok(ingredienteService.alterar(id, ingrediente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ingredienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}