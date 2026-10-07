package com.easyeats.api.repository;

import com.easyeats.api.entity.Preco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrecoRepository
        extends JpaRepository<Preco, Long> {

    List<Preco> findByProdutoId(Long produtoId);

    Optional<Preco>
    findFirstByProdutoIdAndFlativoOrderByIdDesc(
            Long produtoId,
            String flativo
    );
}