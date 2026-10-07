package com.easyeats.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProdutoDto(

        Long id,

        @NotBlank(message = "O nome é obrigatório")
        String nome,

        String descricao,

        String flativo,

        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId,

        @NotNull(message = "O preço é obrigatório")
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal preco

) {
}