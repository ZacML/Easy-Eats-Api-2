package com.easyeats.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProdutoIngredienteLoteDto(

        @NotNull(message = "O produto é obrigatório")
        Long produtoId,

        @NotEmpty(message = "Informe ao menos um ingrediente")
        @Valid
        List<IngredienteQuantidadeDto> ingredientes
) {
}