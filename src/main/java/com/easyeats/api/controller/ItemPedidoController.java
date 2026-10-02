package com.easyeats.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.easyeats.api.dto.ItemPedidoDto;
import com.easyeats.api.service.ItemPedidoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/item-pedido")
@RequiredArgsConstructor
public class ItemPedidoController {

    private final ItemPedidoService service;

    @PostMapping("/pedido/{pedidoId}")
    public ResponseEntity<ItemPedidoDto> adicionar(
            @PathVariable Long pedidoId,
            @Valid @RequestBody ItemPedidoDto item) {

        ItemPedidoDto salvo = service.adicionar(pedidoId, item);
        if (salvo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<ItemPedidoDto>> listarPorPedido(@PathVariable Long pedidoId) {
        List<ItemPedidoDto> itens = service.listarPorPedido(pedidoId);
        if (itens.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(itens);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemPedidoDto> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemPedidoDto> alterar(
            @PathVariable Long id,
            @Valid @RequestBody ItemPedidoDto item) {

        ItemPedidoDto atualizado = service.alterar(id, item);
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