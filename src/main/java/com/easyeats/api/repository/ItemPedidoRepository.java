package com.easyeats.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.easyeats.api.entity.ItemPedido;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    List<ItemPedido> findByPedidoId(Long pedidoId);

    long countByPedidoId(Long pedidoId);
}