package com.easyeats.api.dto.mapper;

import com.easyeats.api.dto.CategoriaDto;
import com.easyeats.api.entity.Categoria;

public class CategoriaMapper {

    public static Categoria toEntity(CategoriaDto dto) {

        Categoria categoria = new Categoria();

        categoria.setNome(dto.nome());
        categoria.setFlativo(dto.flativo());

        return categoria;
    }

    public static CategoriaDto toDto(Categoria categoria) {

        return new CategoriaDto(
                categoria.getId(),
                categoria.getNome(),
                categoria.getFlativo()
        );
    }
}