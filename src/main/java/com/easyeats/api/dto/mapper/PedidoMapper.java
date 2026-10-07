package com.easyeats.api.dto.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.easyeats.api.dto.PedidoDto;
import com.easyeats.api.entity.ItemPedido;
import com.easyeats.api.entity.Mesa;
import com.easyeats.api.entity.Pedido;
import com.easyeats.api.entity.StatusPedidoEnum;
import com.easyeats.api.repository.MesaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PedidoMapper {

    private final ItemPedidoMapper itemMapper;
    private final MesaRepository mesaRepository;

    public PedidoDto toDto(Pedido pedido) {
        return new PedidoDto(
                pedido.getId(),
                pedido.getDataCriacao(),
                pedido.getDataAlteracao(),
                pedido.getMesa() != null ? pedido.getMesa().getId() : null,
                pedido.getCliente(),
                pedido.getStatus() == null
                        ? StatusPedidoEnum.ABERTO.name()
                        : pedido.getStatus().name(),
                pedido.getItens().stream()
                        .map(itemMapper::toDto)
                        .toList()
        );
    }

    public Pedido toEntity(PedidoDto dto, List<ItemPedido> itens) {
        Pedido pedido = new Pedido();

        pedido.setMesa(buscarMesa(dto.mesaId()));
        pedido.setCliente(dto.cliente());

        if (dto.status() != null && !dto.status().isBlank()) {
            pedido.setStatus(StatusPedidoEnum.valueOf(dto.status().toUpperCase()));
        }

        itens.forEach(pedido::adicionarItem);
        return pedido;
    }

    private Mesa buscarMesa(Long mesaId) {
        if (mesaId == null) {
            throw new IllegalArgumentException("A mesa é obrigatória");
        }

        return mesaRepository.findById(mesaId)
                .filter(mesa -> "S".equals(mesa.getFlativo()))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Mesa não encontrada ou inativa: " + mesaId
                        ));
    }
}
