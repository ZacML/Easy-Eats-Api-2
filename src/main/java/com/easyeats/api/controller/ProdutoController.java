package com.easyeats.api.controller;

import com.easyeats.api.dto.ProdutoDto;
import com.easyeats.api.service.ProdutoService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produto")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService service;


    @PostMapping
    public ResponseEntity<ProdutoDto> criar(
            @Valid
            @RequestBody
            ProdutoDto produto
    ) {

        ProdutoDto salvo =
                service.salvar(produto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(salvo);
    }


    @GetMapping
    public ResponseEntity<List<ProdutoDto>> listarTodos() {

        List<ProdutoDto> produtos =
                service.listarTodos();

        return ResponseEntity.ok(produtos);
    }


    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto> buscarPorId(
            @PathVariable Long id
    ) {

        return service.buscarPorId(id)

                .map(ResponseEntity::ok)

                .orElse(
                        ResponseEntity.notFound()
                                .build()
                );
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDto> alterar(

            @PathVariable Long id,

            @Valid
            @RequestBody
            ProdutoDto produto
    ) {

        ProdutoDto atualizado =
                service.alterar(
                        id,
                        produto
                );

        if (atualizado == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(
                atualizado
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id
    ) {

        service.excluir(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}