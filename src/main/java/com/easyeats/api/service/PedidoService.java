package com.easyeats.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
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
    private final EstoqueService estoqueService;

    @Transactional
    public PedidoDto salvar(PedidoDto dto) {

        List<ItemPedido> itens = montarItens(dto);
        estoqueService.validarDisponibilidade(itens);

        Pedido pedido = mapper.toEntity(dto, itens);

        // cálculo do valorTotal
        BigDecimal valorTotal = itens.stream()
                .map(item -> item.getValorUnitario()
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setValorTotal(valorTotal);

        // pedido já criado em preparo: a baixa acontece agora, na mesma transação
        if (pedido.getStatus() == StatusPedidoEnum.EM_PREPARO) {
            definirStatus(pedido, StatusPedidoEnum.EM_PREPARO);
        }

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

        // se o estoque do pedido já foi baixado, os novos itens não passam por nova validação
        if (!pedido.isEstoqueBaixado()) {
            estoqueService.validarDisponibilidade(itens);
        }

        pedido.setMesa(mesaRepository.findById(dto.mesaId())
                .filter(mesa -> "S".equals(mesa.getFlativo()))
                .orElseThrow(() -> new RuntimeException("Mesa não encontrada ou inativa")));
        pedido.setCliente(dto.cliente());

        if (dto.status() != null && !dto.status().isBlank()) {
            pedido.setStatus(StatusPedidoEnum.valueOf(dto.status().toUpperCase()));
        }

        pedido.getItens().clear();
        itens.forEach(pedido::adicionarItem);

        // depois de trocar os itens, para a baixa (se houver) usar os itens novos
        if (dto.status() != null && !dto.status().isBlank()) {
            definirStatus(pedido, StatusPedidoEnum.valueOf(dto.status().toUpperCase()));
        }

        return mapper.toDto(repository.save(pedido));
    }

    @Transactional
    public PedidoDto atualizarStatus(Long id, String status) {
        // lock no pedido: duas chamadas simultâneas para o mesmo pedido são executadas uma por vez
        Pedido pedido = repository.buscarPorIdParaAtualizar(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        definirStatus(pedido, StatusPedidoEnum.valueOf(status.toUpperCase()));
        return mapper.toDto(repository.save(pedido));
    }
    
    /**
     * Único ponto que altera o status. Ao entrar em EM_PREPARO baixa os ingredientes do estoque,
     * uma única vez por pedido (flag estoqueBaixado). Se faltar estoque, lança exceção e nada é alterado.
     */
    private void definirStatus(Pedido pedido, StatusPedidoEnum novoStatus) {
        if (novoStatus == StatusPedidoEnum.EM_PREPARO && !pedido.isEstoqueBaixado()) {
            estoqueService.baixarPorItens(pedido.getItens());
            pedido.setEstoqueBaixado(true);
        }
        pedido.setStatus(novoStatus);
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

    private static final int LIMITE_HISTORICO = 10;

    @Transactional(readOnly = true)
    public List<PedidoDto> listarPorStatus(StatusPedidoEnum status, LocalDateTime antesDe) {
        List<Pedido> pedidos;

        if (status == StatusPedidoEnum.FINALIZADO || status == StatusPedidoEnum.CANCELADO) {
            LocalDate hoje = LocalDate.now();
            LocalDateTime fim = antesDe != null ? antesDe : hoje.plusDays(1).atStartOfDay();
            pedidos = repository.buscarHistorico(
                    status, hoje.atStartOfDay(), fim, PageRequest.of(0, LIMITE_HISTORICO));
        } else {
            pedidos = repository.findByStatusOrderByDataCriacaoAsc(status);
        }

        return pedidos.stream().map(mapper::toDto).toList();
    }
}
