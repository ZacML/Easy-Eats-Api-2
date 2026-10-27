package com.easyeats.api.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.easyeats.api.entity.StatusPedidoEnum;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.easyeats.api.dto.PedidoDto;
import com.easyeats.api.service.PedidoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pedido")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    @PostMapping
    public ResponseEntity<PedidoDto> criar(@Valid @RequestBody PedidoDto pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(pedido));
    }

    @GetMapping
    public ResponseEntity<List<PedidoDto>> listarTodos() {
        List<PedidoDto> pedidos = service.listarTodos();
        if (pedidos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDto> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoDto> alterar(
            @PathVariable Long id,
            @Valid @RequestBody PedidoDto pedido) {

        PedidoDto atualizado = service.alterar(id, pedido);
        if (atualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(atualizado);
    }

    @PatchMapping("/{id}/status/{status}")
    public ResponseEntity<PedidoDto> atualizarStatus(
            @PathVariable Long id,
            @PathVariable String status) {
        return ResponseEntity.ok(service.atualizarStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(params = "status")
    public ResponseEntity<List<PedidoDto>> listarPorStatus(
            @RequestParam StatusPedidoEnum status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime antesDe) {
        return ResponseEntity.ok(service.listarPorStatus(status, antesDe));
    }
}
