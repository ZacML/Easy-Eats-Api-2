package com.easyeats.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Na entrada, apenas produtoId e quantidade são usados.
 * id, pedidoId e valorUnitario são preenchidos pela API (o valor vem do preço ativo do produto).
 */
public record ItemPedidoDto(
        Long id,

        Long pedidoId,

        @NotNull(message = "O produto é obrigatório")
        Long produtoId,

        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        BigDecimal valorUnitario
) {
}