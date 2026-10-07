package com.easyeats.api.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.easyeats.api.dto.MesaDto;
import com.easyeats.api.service.MesaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mesa")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService service;

    @PostMapping
    public ResponseEntity<MesaDto> criar(@Valid @RequestBody MesaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(dto));
    }

    @GetMapping
    public ResponseEntity<List<MesaDto>> listar() {
        List<MesaDto> mesas = service.listarAtivas();

        if (mesas.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(mesas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MesaDto> buscar(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<MesaDto> alterar(
            @PathVariable Long id,
            @Valid @RequestBody MesaDto dto) {

        MesaDto atualizado = service.alterar(id, dto);

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
