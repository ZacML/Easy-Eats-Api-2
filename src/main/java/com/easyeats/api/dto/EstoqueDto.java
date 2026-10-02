package com.easyeats.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record EstoqueDto(

        Long id,

        @NotNull(message = "A quantidade atual é obrigatória")
        @PositiveOrZero(message = "A quantidade atual não pode ser negativa")
        Integer qtdAtual,

        @NotNull(message = "A quantidade mínima é obrigatória")
        @PositiveOrZero(message = "A quantidade mínima não pode ser negativa")
        Integer qtdMinima,

        LocalDateTime dtAtualizacao,

        @NotNull(message = "O ingrediente é obrigatório")
        Long idIngrediente,

        String nomeIngrediente,

        Boolean abaixoDoMinimo      // calculado pela API, o que vier do cliente é ignorado
) {
}