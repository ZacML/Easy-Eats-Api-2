package com.easyeats.api.dto.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.easyeats.api.dto.ItemPedidoDto;
import com.easyeats.api.entity.ItemPedido;
import com.easyeats.api.entity.Produto;

@Component
public class ItemPedidoMapper {

    public ItemPedidoDto toDto(ItemPedido item) {
        return new ItemPedidoDto(
                item.getId(),
                item.getPedido().getId(),
                item.getProduto().getId(),
                item.getQuantidade(),
                item.getValorUnitario()
        );
    }

    public ItemPedido toEntity(ItemPedidoDto dto, Produto produto, BigDecimal valorUnitario) {
        ItemPedido item = new ItemPedido();
        item.setProduto(produto);
        item.setQuantidade(dto.quantidade());
        item.setValorUnitario(valorUnitario);
        return item;
    }
}