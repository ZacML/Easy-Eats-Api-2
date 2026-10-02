package com.easyeats.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.easyeats.api.dto.PedidoDto;
import com.easyeats.api.dto.mapper.PedidoMapper;
import com.easyeats.api.entity.ItemPedido;
import com.easyeats.api.entity.Pedido;
import com.easyeats.api.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository repository;
    private final ItemPedidoService itemPedidoService;
    private final PedidoMapper mapper;

    @Transactional
    public PedidoDto salvar(PedidoDto dto) {

        List<ItemPedido> itens = montarItens(dto);

        return mapper.toDto(repository.save(mapper.toEntity(itens)));
    }

    @Transactional
    public PedidoDto alterar(Long id, PedidoDto dto) {

        Optional<Pedido> pedidoExistente = repository.findById(id);

        if (pedidoExistente.isEmpty()) {
            return null;
        }

        List<ItemPedido> itens = montarItens(dto);

        // Substitui os itens do pedido (os antigos são removidos por orphanRemoval)
        Pedido pedido = pedidoExistente.get();
        pedido.getItens().clear();
        itens.forEach(pedido::adicionarItem);

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