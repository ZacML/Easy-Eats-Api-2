package com.easyeats.api.repository;

import com.easyeats.api.entity.Estoque;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByIngredienteId(Long idIngrediente);

    boolean existsByIngredienteId(Long idIngrediente);

    @Query("select e from Estoque e where e.qtdAtual <= e.qtdMinima and e.ingrediente.flativo = 'S'")
    List<Estoque> findAbaixoDoMinimo();

    // Consulta simples (sem lock), usada só para validar a inclusão de itens
    List<Estoque> findByIngredienteIdIn(Collection<Long> idsIngredientes);

    // Com lock e em ordem fixa de id (evita deadlock) para a baixa definitiva
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Estoque> findByIngredienteIdInOrderByIdAsc(Collection<Long> idsIngredientes);
}
