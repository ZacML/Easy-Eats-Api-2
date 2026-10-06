package com.easyeats.api.dto.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.easyeats.api.dto.PedidoDto;
import com.easyeats.api.entity.ItemPedido;
import com.easyeats.api.entity.Pedido;
import com.easyeats.api.entity.StatusPedidoEnum;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PedidoMapper {

    private final ItemPedidoMapper itemMapper;

    public PedidoDto toDto(Pedido pedido) {
        return new PedidoDto(
                pedido.getId(),
                pedido.getDataCriacao(),
                pedido.getDataAlteracao(),
                pedido.getMesa(),
                pedido.getCliente(),
                pedido.getStatus() == null ? StatusPedidoEnum.ABERTO.name() : pedido.getStatus().name(),
                pedido.getItens().stream()
                        .map(itemMapper::toDto)
                        .toList()
        );
    }

    public Pedido toEntity(PedidoDto dto, List<ItemPedido> itens) {
        Pedido pedido = new Pedido();
        pedido.setMesa(dto.mesa());
        pedido.setCliente(dto.cliente());

        if (dto.status() != null && !dto.status().isBlank()) {
            pedido.setStatus(StatusPedidoEnum.valueOf(dto.status().toUpperCase()));
        }

        itens.forEach(pedido::adicionarItem);
        return pedido;
    }
}
