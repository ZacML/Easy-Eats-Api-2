package com.easyeats.api.controller;

import com.easyeats.api.dto.PrecoDto;
import com.easyeats.api.service.PrecoService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/preco")
@RequiredArgsConstructor
public class PrecoController {

    private final PrecoService service;


    @PostMapping
    public ResponseEntity<PrecoDto> criar(
            @Valid
            @RequestBody
            PrecoDto preco
    ) {

        PrecoDto salvo =
                service.salvar(preco);

        if (salvo == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(salvo);
    }


    @GetMapping
    public ResponseEntity<List<PrecoDto>> listarTodos() {

        return ResponseEntity.ok(
                service.listarTodos()
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<PrecoDto> buscarPorId(
            @PathVariable Long id
    ) {

        return service.buscarPorId(id)

                .map(ResponseEntity::ok)

                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }


    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<List<PrecoDto>> listarPorProduto(
            @PathVariable Long produtoId
    ) {

        return ResponseEntity.ok(
                service.listarPorProduto(
                        produtoId
                )
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<PrecoDto> alterar(
            @PathVariable Long id,

            @Valid
            @RequestBody
            PrecoDto preco
    ) {

        /*
         * Para alteração de preço,
         * é melhor criar uma nova versão
         * do preço em vez de sobrescrever
         * o histórico.
         */
        PrecoDto atualizado =
                service.salvar(
                        preco
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