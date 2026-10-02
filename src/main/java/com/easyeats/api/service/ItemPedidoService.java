package com.easyeats.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.easyeats.api.dto.ItemPedidoDto;
import com.easyeats.api.dto.mapper.ItemPedidoMapper;
import com.easyeats.api.entity.ItemPedido;
import com.easyeats.api.entity.Pedido;
import com.easyeats.api.entity.Preco;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.repository.ItemPedidoRepository;
import com.easyeats.api.repository.PedidoRepository;
import com.easyeats.api.repository.PrecoRepository;
import com.easyeats.api.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemPedidoService {

    private final ItemPedidoRepository repository;
    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final PrecoRepository precoRepository;
    private final ItemPedidoMapper mapper;

    @Transactional
    public ItemPedidoDto adicionar(Long pedidoId, ItemPedidoDto dto) {

        Optional<Pedido> pedido = pedidoRepository.findById(pedidoId);

        if (pedido.isEmpty()) {
            return null;
        }

        ItemPedido item = montarItem(dto);
        pedido.get().adicionarItem(item);

        return mapper.toDto(repository.save(item));
    }

    @Transactional
    public ItemPedidoDto alterar(Long id, ItemPedidoDto dto) {

        Optional<ItemPedido> itemExistente = repository.findById(id);

        if (itemExistente.isEmpty()) {
            return null;
        }

        ItemPedido item = itemExistente.get();
        item.setQuantidade(dto.quantidade());

        // Se o produto mudou, o valor é recalculado; senão mantém o valor da compra original
        if (!item.getProduto().getId().equals(dto.produtoId())) {
            Produto produto = buscarProdutoAtivo(dto.produtoId());
            item.setProduto(produto);
            item.setValorUnitario(buscarPrecoAtual(produto));
        }

        return mapper.toDto(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<ItemPedidoDto> listarPorPedido(Long pedidoId) {
        return repository.findByPedidoId(pedidoId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ItemPedidoDto> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDto);
    }

    @Transactional
    public void excluir(Long id) {

        Optional<ItemPedido> item = repository.findById(id);

        if (item.isEmpty()) {
            return;
        }

        if (repository.countByPedidoId(item.get().getPedido().getId()) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O pedido deve ter ao menos um item");
        }

        repository.delete(item.get());
    }

    /**
     * Monta um item (ainda sem pedido) validando o produto e fixando o valor
     * unitário a partir do preço ativo mais recente. Usado também pelo PedidoService.
     */
    public ItemPedido montarItem(ItemPedidoDto dto) {
        Produto produto = buscarProdutoAtivo(dto.produtoId());
        return mapper.toEntity(dto, produto, buscarPrecoAtual(produto));
    }

    private Produto buscarProdutoAtivo(Long produtoId) {
        return produtoRepository.findById(produtoId)
                .filter(produto -> "S".equals(produto.getFlativo()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Produto " + produtoId + " não encontrado ou inativo"));
    }

    private BigDecimal buscarPrecoAtual(Produto produto) {
        return precoRepository.findFirstByProdutoIdAndFlativoOrderByIdDesc(produto.getId(), "S")
                .map(Preco::getValor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Produto " + produto.getId() + " não possui preço ativo"));
    }
}