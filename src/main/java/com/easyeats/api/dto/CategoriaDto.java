package com.easyeats.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaDto(
        Long id,
        @NotBlank(message = "O nome é obrigatório")
        String nome,
        String flativo
) {
}