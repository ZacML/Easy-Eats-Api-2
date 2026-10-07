package com.easyeats.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record PedidoDto(
        Long id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAlteracao,
        Long mesaId,
        String cliente,
        String status,

        @NotEmpty(message = "O pedido deve ter ao menos um item")
        @Valid
        List<ItemPedidoDto> itens
) {
}
