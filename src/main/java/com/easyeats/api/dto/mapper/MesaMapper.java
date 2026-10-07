package com.easyeats.api.dto.mapper;

import org.springframework.stereotype.Component;

import com.easyeats.api.dto.MesaDto;
import com.easyeats.api.entity.Mesa;

@Component
public class MesaMapper {

    public MesaDto toDto(Mesa mesa) {
        return new MesaDto(
                mesa.getId(),
                mesa.getNumero(),
                mesa.getFlativo()
        );
    }

    public Mesa toEntity(MesaDto dto) {
        Mesa mesa = new Mesa();
        mesa.setNumero(dto.numero());
        mesa.setFlativo(dto.flativo());
        return mesa;
    }
}
