package com.easyeats.api.dto.mapper;

import com.easyeats.api.dto.IngredienteQuantidadeDto;
import com.easyeats.api.dto.ProdutoIngredienteDto;
import com.easyeats.api.dto.ProdutoIngredienteLoteDto;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.entity.ProdutoIngrediente;
import com.easyeats.api.entity.ProdutoIngredienteId;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProdutoIngredienteMapper {

    public ProdutoIngredienteDto toDto(ProdutoIngrediente produtoIngrediente) {
        if (produtoIngrediente == null) {
            return null;
        }

        return new ProdutoIngredienteDto(
                produtoIngrediente.getId().getProdutoId(),
                produtoIngrediente.getId().getIngredienteId(),
                produtoIngrediente.getQuantidade()
        );
    }

    public ProdutoIngrediente toEntity(Produto produto, Ingrediente ingrediente, Double quantidade) {
        ProdutoIngrediente produtoIngrediente = new ProdutoIngrediente();

        produtoIngrediente.setId(
                new ProdutoIngredienteId(produto.getId(), ingrediente.getId())
        );
        produtoIngrediente.setProduto(produto);
        produtoIngrediente.setIngrediente(ingrediente);
        produtoIngrediente.setQuantidade(quantidade);

        return produtoIngrediente;
    }

    public ProdutoIngredienteLoteDto toLoteDto(Long produtoId, List<ProdutoIngrediente> vinculos) {
        List<IngredienteQuantidadeDto> ingredientes = vinculos.stream()
                .map(v -> new IngredienteQuantidadeDto(
                        v.getId().getIngredienteId(),
                        v.getQuantidade()))
                .toList();

        return new ProdutoIngredienteLoteDto(produtoId, ingredientes);
    }
}