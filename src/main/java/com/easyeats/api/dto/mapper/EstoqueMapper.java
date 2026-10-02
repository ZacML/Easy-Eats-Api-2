package com.easyeats.api.dto.mapper;

import com.easyeats.api.dto.EstoqueDto;
import com.easyeats.api.entity.Estoque;
import org.springframework.stereotype.Component;

@Component
public class EstoqueMapper {

    public EstoqueDto toDto(Estoque estoque) {
        if (estoque == null) {
            return null;
        }

        Long idIngrediente = null;
        String nomeIngrediente = null;

        if (estoque.getIngrediente() != null) {
            idIngrediente = estoque.getIngrediente().getId();
            nomeIngrediente = estoque.getIngrediente().getNome();
        }

        boolean abaixoDoMinimo = estoque.getQtdAtual() <= estoque.getQtdMinima();   // novo

        return new EstoqueDto(
                estoque.getId(),
                estoque.getQtdAtual(),
                estoque.getQtdMinima(),
                estoque.getDtAtualizacao(),
                idIngrediente,
                nomeIngrediente,
                abaixoDoMinimo                                                         // novo
        );
    }

    public Estoque toEntity(EstoqueDto dto) {

        if (dto == null) {
            return null;
        }

        Estoque estoque = new Estoque();

        estoque.setQtdAtual(dto.qtdAtual());
        estoque.setQtdMinima(dto.qtdMinima());

        return estoque;
    }
}