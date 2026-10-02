package com.easyeats.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.easyeats.api.entity.Preco;

public interface PrecoRepository extends JpaRepository<Preco, Long> {

    List<Preco> findByProdutoId(Long produtoId);

    Optional<Preco> findFirstByProdutoIdAndFlativoOrderByIdDesc(Long produtoId, String flativo);
}