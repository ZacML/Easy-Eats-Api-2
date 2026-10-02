package com.easyeats.api.dto.mapper;

import com.easyeats.api.dto.IngredienteDto;
import com.easyeats.api.entity.Ingrediente;
import org.springframework.stereotype.Component;

@Component
public class IngredienteMapper {
    public IngredienteDto toDto(Ingrediente ingrediente) {
        if (ingrediente == null) {
            return null;
        }

        return new IngredienteDto(
                ingrediente.getId(),
                ingrediente.getNome(),
                ingrediente.getUnidadeMedida(),
                ingrediente.getCusto()
        );
    }

    public Ingrediente toEntity(IngredienteDto dto) {
        if (dto == null) {
            return null;
        }

        Ingrediente ingrediente = new Ingrediente();


        ingrediente.setNome(dto.nome());
        ingrediente.setUnidadeMedida(dto.unidadeMedida());
        ingrediente.setCusto(dto.custo());

        return ingrediente;
    }
}
