package com.easyeats.api.dto.mapper;

import com.easyeats.api.dto.ProdutoDto;
import com.easyeats.api.entity.Categoria;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.repository.CategoriaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor 
public class ProdutoMapper {

    private final CategoriaRepository categoriaRepository;

    public ProdutoDto toDto(Produto produto) {
        return new ProdutoDto(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getFlativo(),
                produto.getCategoria() != null
                        ? produto.getCategoria().getId()
                        : null
        );
    }

    public Produto toEntity(ProdutoDto dto) {

        Produto produto = new Produto();

        produto.setNome(dto.nome());
        produto.setDescricao(dto.descricao());
        produto.setFlativo(dto.flativo());

        produto.setCategoria(
                buscarCategoria(dto.categoriaId())
        );

        return produto;
    }

    private Categoria buscarCategoria(Long categoriaId) {

        if (categoriaId == null) {
            return null;
        }

        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Categoria não encontrada: " + categoriaId
                        )
                );
    }
}