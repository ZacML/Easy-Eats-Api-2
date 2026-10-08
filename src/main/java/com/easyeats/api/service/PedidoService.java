package com.easyeats.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.easyeats.api.dto.PedidoDto;
import com.easyeats.api.dto.mapper.PedidoMapper;
import com.easyeats.api.entity.ItemPedido;
import com.easyeats.api.entity.Pedido;
import com.easyeats.api.entity.StatusPedidoEnum;
import com.easyeats.api.repository.PedidoRepository;
import com.easyeats.api.repository.MesaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository repository;
    private final ItemPedidoService itemPedidoService;
    private final PedidoMapper mapper;
    private final MesaRepository mesaRepository;

    @Transactional
    public PedidoDto salvar(PedidoDto dto) {

        List<ItemPedido> itens = montarItens(dto);

        Pedido pedido = mapper.toEntity(dto, itens);

        BigDecimal valorTotal = itens.stream()
                .map(item -> item.getValorUnitario()
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setValorTotal(valorTotal);

        return mapper.toDto(repository.save(pedido));
    }

    @Transactional
    public PedidoDto alterar(Long id, PedidoDto dto) {
        Optional<Pedido> pedidoExistente = repository.findById(id);

        if (pedidoExistente.isEmpty()) {
            return null;
        }

        Pedido pedido = pedidoExistente.get();
        List<ItemPedido> itens = montarItens(dto);

        pedido.setMesa(mesaRepository.findById(dto.mesaId())
                .filter(mesa -> "S".equals(mesa.getFlativo()))
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada ou inativa")));
        pedido.setCliente(dto.cliente());

        if (dto.status() != null && !dto.status().isBlank()) {
            pedido.setStatus(StatusPedidoEnum.valueOf(dto.status().toUpperCase()));
        }

        pedido.getItens().clear();
        itens.forEach(pedido::adicionarItem);

        return mapper.toDto(repository.save(pedido));
    }

    @Transactional
    public PedidoDto atualizarStatus(Long id, String status) {
        Pedido pedido = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatus(StatusPedidoEnum.valueOf(status.toUpperCase()));
        return mapper.toDto(repository.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> listarTodos() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PedidoDto> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Transactional
    public void excluir(Long id) {
        repository.findById(id).ifPresent(repository::delete);
    }

    private List<ItemPedido> montarItens(PedidoDto dto) {
        return dto.itens().stream()
                .map(itemPedidoService::montarItem)
                .toList();
    }
}
