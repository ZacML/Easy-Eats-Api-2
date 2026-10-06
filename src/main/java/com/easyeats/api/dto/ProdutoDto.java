package com.easyeats.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProdutoDto(
        Long id,

        @NotBlank(message = "O nome é obrigatório")
        String nome,

        String descricao,

        String flativo,

        @NotNull(message = "A categoria é obrigatória")
        Long categoriaId
) {
}