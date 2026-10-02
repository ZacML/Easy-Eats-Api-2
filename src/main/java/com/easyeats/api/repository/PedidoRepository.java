package com.easyeats.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.easyeats.api.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Carrega os itens junto na mesma consulta (evita 1 query extra por pedido)
    @Override
    @EntityGraph(attributePaths = "itens")
    List<Pedido> findAll();
}