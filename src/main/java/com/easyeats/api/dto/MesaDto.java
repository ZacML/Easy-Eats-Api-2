package com.easyeats.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MesaDto(
        Long id,

        @NotNull(message = "O número da mesa é obrigatório")
        @Positive(message = "O número da mesa deve ser maior que zero")
        Integer numero,

        String flativo
) {
}
