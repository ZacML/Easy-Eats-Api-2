package com.easyeats.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.easyeats.api.entity.StatusPedidoEnum;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.easyeats.api.entity.Pedido;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Carrega os itens junto na mesma consulta (evita 1 query extra por pedido)
    @Override
    @EntityGraph(attributePaths = "itens")
    List<Pedido> findAll();

    // Bloqueia a linha do pedido até o fim da transação (evita baixa duplicada em chamadas simultâneas)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> buscarPorIdParaAtualizar(@Param("id") Long id);

    // Fila: todos, mais antigos primeiro
    @EntityGraph(attributePaths = "itens")
    List<Pedido> findByStatusOrderByDataCriacaoAsc(StatusPedidoEnum status);

    // Histórico: pedidos do período, mais recentes primeiro
    @Query("""
        select p from Pedido p
        where p.status = :status
          and p.dataAlteracao >= :inicio
          and p.dataAlteracao < :fim
        order by p.dataAlteracao desc
        """)
    List<Pedido> buscarHistorico(
            @Param("status") StatusPedidoEnum status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            Pageable pageable);
}