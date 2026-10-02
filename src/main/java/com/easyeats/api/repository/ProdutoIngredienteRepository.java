package com.easyeats.api.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.easyeats.api.entity.ProdutoIngrediente;
import com.easyeats.api.entity.ProdutoIngredienteId;

@Repository
public interface ProdutoIngredienteRepository
        extends JpaRepository<ProdutoIngrediente, ProdutoIngredienteId> {
    boolean existsByIngredienteIdAndProdutoFlativo(Long ingredienteId, String flativo);

}