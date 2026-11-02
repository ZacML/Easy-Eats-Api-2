package com.easyeats.api.repository;


import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.easyeats.api.entity.ProdutoIngrediente;
import com.easyeats.api.entity.ProdutoIngredienteId;

public interface ProdutoIngredienteRepository
        extends JpaRepository<ProdutoIngrediente, ProdutoIngredienteId> {
    boolean existsByIngredienteIdAndProdutoFlativo(Long ingredienteId, String flativo);

    @Query("select pi from ProdutoIngrediente pi join fetch pi.ingrediente where pi.produto.id in :produtoIds")
    List<ProdutoIngrediente> buscarPorProdutos(@Param("produtoIds") Collection<Long> produtoIds);

}