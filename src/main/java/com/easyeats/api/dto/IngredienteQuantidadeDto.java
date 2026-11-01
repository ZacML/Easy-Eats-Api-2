package com.easyeats.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IngredienteQuantidadeDto(

        @NotNull(message = "O ingrediente é obrigatório")
        Long ingredienteId,

        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        Double quantidade
) {
}