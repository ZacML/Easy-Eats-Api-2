package com.easyeats.api.dto.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.easyeats.api.dto.PedidoDto;
import com.easyeats.api.entity.ItemPedido;
import com.easyeats.api.entity.Pedido;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PedidoMapper {

    private final ItemPedidoMapper itemMapper;

    public PedidoDto toDto(Pedido pedido) {
        return new PedidoDto(
                pedido.getId(),
                pedido.getDataCriacao(),
                pedido.getItens().stream()
                        .map(itemMapper::toDto)
                        .toList()
        );
    }

    public Pedido toEntity(List<ItemPedido> itens) {
        Pedido pedido = new Pedido();
        itens.forEach(pedido::adicionarItem);
        return pedido;
    }
}