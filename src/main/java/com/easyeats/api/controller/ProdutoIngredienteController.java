package com.easyeats.api.controller;

import com.easyeats.api.dto.ProdutoIngredienteDto;
import com.easyeats.api.service.ProdutoIngredienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produto-ingrediente")
@RequiredArgsConstructor
public class ProdutoIngredienteController {

    private final ProdutoIngredienteService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoIngredienteDto salvar(
            @Valid
            @RequestBody ProdutoIngredienteDto dto) {

        return service.salvar(dto);
    }

    @GetMapping
    public List<ProdutoIngredienteDto> listar() {
        return service.listar();
    }

    @GetMapping("/{produtoId}/{ingredienteId}")
    public ProdutoIngredienteDto buscarPorId(
            @PathVariable Long produtoId,
            @PathVariable Long ingredienteId) {

        return service.buscarPorId(produtoId, ingredienteId);
    }

    @DeleteMapping("/{produtoId}/{ingredienteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @PathVariable Long produtoId,
            @PathVariable Long ingredienteId) {

        service.excluir(produtoId, ingredienteId);
    }
}