package com.easyeats.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record IngredienteDto(
        Long id,
        @NotBlank(message = "O nome do ingrediente é obrigatório") String nome,
        @NotBlank(message = "A unidade de medida é obrigatória") String unidadeMedida,
        @NotNull(message = "O custo é obrigatório")
        @PositiveOrZero(message = "O custo não pode ser negativo")
        BigDecimal custo
) {
}
