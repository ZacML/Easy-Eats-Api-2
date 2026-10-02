package com.easyeats.api.controller;

import com.easyeats.api.dto.EstoqueDto;
import com.easyeats.api.dto.MovimentacaoDto;
import com.easyeats.api.service.EstoqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estoques")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @PostMapping
    public ResponseEntity<EstoqueDto> criar(@Valid @RequestBody EstoqueDto estoque) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estoqueService.salvar(estoque));
    }

    @GetMapping
    public ResponseEntity<List<EstoqueDto>> listarTodos() {
        return ResponseEntity.ok(estoqueService.listarTodos());
    }

    @GetMapping("/alertas")
    public ResponseEntity<List<EstoqueDto>> listarAlertas() {
        return ResponseEntity.ok(estoqueService.listarAlertas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstoqueDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estoqueService.buscarPorId(id));
    }

    @GetMapping("/ingrediente/{idIngrediente}")
    public ResponseEntity<EstoqueDto> buscarPorIngrediente(@PathVariable Long idIngrediente) {
        return ResponseEntity.ok(estoqueService.buscarPorIngrediente(idIngrediente));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstoqueDto> alterar(@PathVariable Long id,
                                              @Valid @RequestBody EstoqueDto estoque) {
        return ResponseEntity.ok(estoqueService.alterar(id, estoque));
    }

    @PatchMapping("/{id}/entrada")
    public ResponseEntity<EstoqueDto> entrada(@PathVariable Long id,
                                              @Valid @RequestBody MovimentacaoDto mov) {
        return ResponseEntity.ok(estoqueService.registrarEntrada(id, mov.quantidade()));
    }

    @PatchMapping("/{id}/saida")
    public ResponseEntity<EstoqueDto> saida(@PathVariable Long id,
                                            @Valid @RequestBody MovimentacaoDto mov) {
        return ResponseEntity.ok(estoqueService.registrarSaida(id, mov.quantidade()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        estoqueService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}