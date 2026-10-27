package com.easyeats.api.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.easyeats.api.entity.StatusPedidoEnum;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.easyeats.api.entity.Pedido;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Carrega os itens junto na mesma consulta (evita 1 query extra por pedido)
    @Override
    @EntityGraph(attributePaths = "itens")
    List<Pedido> findAll();

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