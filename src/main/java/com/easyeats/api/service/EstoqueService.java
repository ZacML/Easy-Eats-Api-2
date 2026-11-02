package com.easyeats.api.service;

import com.easyeats.api.dto.EstoqueDto;
import com.easyeats.api.dto.mapper.EstoqueMapper;
import com.easyeats.api.entity.Estoque;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.entity.ItemPedido;
import com.easyeats.api.entity.ProdutoIngrediente;
import com.easyeats.api.exception.RecursoNaoEncontradoException;
import com.easyeats.api.exception.RegraNegocioException;
import com.easyeats.api.repository.EstoqueRepository;
import com.easyeats.api.repository.IngredienteRepository;
import com.easyeats.api.repository.ProdutoIngredienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final IngredienteRepository ingredienteRepository;
    private final ProdutoIngredienteRepository produtoIngredienteRepository;
    private final EstoqueMapper mapper;

    @Transactional
    public EstoqueDto salvar(EstoqueDto dto) {
        Ingrediente ingrediente = ingredienteRepository.findById(dto.idIngrediente())
                .filter(i -> "S".equals(i.getFlativo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ingrediente", dto.idIngrediente()));

        if (estoqueRepository.existsByIngredienteId(ingrediente.getId())) {
            throw new RegraNegocioException("Este ingrediente já possui estoque cadastrado");
        }

        Estoque estoque = mapper.toEntity(dto);
        estoque.setIngrediente(ingrediente);

        return mapper.toDto(estoqueRepository.save(estoque));
    }

    public List<EstoqueDto> listarTodos() {
        return estoqueRepository.findAll().stream()
                .filter(e -> "S".equals(e.getIngrediente().getFlativo()))
                .map(mapper::toDto)
                .toList();
    }

    public List<EstoqueDto> listarAlertas() {
        return estoqueRepository.findAbaixoDoMinimo().stream()
                .map(mapper::toDto)
                .toList();
    }

    public EstoqueDto buscarPorId(Long id) {
        return mapper.toDto(buscarEntidade(id));
    }

    public EstoqueDto buscarPorIngrediente(Long idIngrediente) {
        Estoque estoque = estoqueRepository.findByIngredienteId(idIngrediente)
                .filter(e -> "S".equals(e.getIngrediente().getFlativo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque do ingrediente", idIngrediente));

        return mapper.toDto(estoque);
    }

    // PUT altera somente o mínimo. A quantidade atual só muda por entrada/saída.
    @Transactional
    public EstoqueDto alterar(Long id, EstoqueDto dto) {
        Estoque estoque = buscarEntidade(id);

        if (!estoque.getIngrediente().getId().equals(dto.idIngrediente())) {
            throw new RegraNegocioException("Não é possível trocar o ingrediente de um estoque");
        }
        if (estoque.getQtdAtual().compareTo(dto.qtdAtual()) != 0) {
            throw new RegraNegocioException(
                    "A quantidade atual só pode ser alterada por entrada ou saída de estoque");
        }

        estoque.setQtdMinima(dto.qtdMinima());

        return mapper.toDto(estoqueRepository.save(estoque));
    }

    @Transactional
    public EstoqueDto registrarEntrada(Long id, BigDecimal quantidade) {
        Estoque estoque = buscarEntidade(id);

        estoque.setQtdAtual(estoque.getQtdAtual().add(quantidade));

        return mapper.toDto(estoqueRepository.save(estoque));
    }

    @Transactional
    public EstoqueDto registrarSaida(Long id, BigDecimal quantidade) {
        Estoque estoque = buscarEntidade(id);

        if (estoque.getQtdAtual().compareTo(quantidade) < 0) {
            throw new RegraNegocioException("Estoque insuficiente de " + estoque.getIngrediente().getNome()
                    + ": disponível " + formatar(estoque.getQtdAtual()) + ", solicitado " + formatar(quantidade));
        }

        estoque.setQtdAtual(estoque.getQtdAtual().subtract(quantidade));

        return mapper.toDto(estoqueRepository.save(estoque));
    }

    /**
     * Valida se há estoque para os itens informados. NÃO movimenta o estoque.
     * Usada ao incluir itens em um pedido; a verificação definitiva é refeita em {@link #baixarPorItens}.
     */
    public void validarDisponibilidade(Collection<ItemPedido> itens) {
        Map<Long, Necessidade> necessidades = calcularNecessidades(itens);
        if (necessidades.isEmpty()) {
            return;
        }

        Map<Long, Estoque> estoques = indexarPorIngrediente(
                estoqueRepository.findByIngredienteIdIn(necessidades.keySet()));

        conferirDisponibilidade(necessidades, estoques);
    }

    /**
     * Baixa definitiva dos ingredientes consumidos pelos itens (tudo ou nada, na transação do chamador).
     * Bloqueia as linhas de estoque, revalida a disponibilidade e registra a saída via {@link #registrarSaida}.
     */
    @Transactional
    public void baixarPorItens(Collection<ItemPedido> itens) {
        Map<Long, Necessidade> necessidades = calcularNecessidades(itens);
        if (necessidades.isEmpty()) {
            return;
        }

        Map<Long, Estoque> estoques = indexarPorIngrediente(
                estoqueRepository.findByIngredienteIdInOrderByIdAsc(necessidades.keySet()));

        conferirDisponibilidade(necessidades, estoques);

        necessidades.forEach((idIngrediente, necessidade) ->
                registrarSaida(estoques.get(idIngrediente).getId(), necessidade.quantidade()));
    }

    // consumo total por ingrediente = soma(quantidade da ficha técnica x quantidade pedida)
    private Map<Long, Necessidade> calcularNecessidades(Collection<ItemPedido> itens) {
        Map<Long, Integer> pedidoPorProduto = new HashMap<>();
        for (ItemPedido item : itens) {
            pedidoPorProduto.merge(item.getProduto().getId(), item.getQuantidade(), Integer::sum);
        }
        if (pedidoPorProduto.isEmpty()) {
            return Map.of();
        }

        Map<Long, BigDecimal> totais = new LinkedHashMap<>();
        Map<Long, Ingrediente> ingredientes = new HashMap<>();

        for (ProdutoIngrediente vinculo : produtoIngredienteRepository.buscarPorProdutos(pedidoPorProduto.keySet())) {
            if (vinculo.getQuantidade() == null) {
                continue;
            }
            int qtdPedida = pedidoPorProduto.get(vinculo.getId().getProdutoId());
            BigDecimal consumo = BigDecimal.valueOf(vinculo.getQuantidade())
                    .multiply(BigDecimal.valueOf(qtdPedida));

            Long idIngrediente = vinculo.getId().getIngredienteId();
            totais.merge(idIngrediente, consumo, BigDecimal::add);
            ingredientes.put(idIngrediente, vinculo.getIngrediente());
        }

        // O estoque tem 3 casas decimais: a soma do pedido é arredondada para cima nessa escala
        // (só faz diferença se a ficha técnica tiver mais de 3 casas; nunca subestima o consumo).
        Map<Long, Necessidade> necessidades = new LinkedHashMap<>();
        totais.forEach((idIngrediente, total) -> necessidades.put(idIngrediente,
                new Necessidade(ingredientes.get(idIngrediente), total.setScale(3, RoundingMode.CEILING))));
        return necessidades;
    }

    private Map<Long, Estoque> indexarPorIngrediente(List<Estoque> estoques) {
        return estoques.stream()
                .filter(e -> "S".equals(e.getIngrediente().getFlativo()))
                .collect(Collectors.toMap(e -> e.getIngrediente().getId(), Function.identity()));
    }

    // ingrediente sem estoque cadastrado (ou inativo) conta como disponível = 0
    private void conferirDisponibilidade(Map<Long, Necessidade> necessidades, Map<Long, Estoque> estoques) {
        List<String> faltas = necessidades.entrySet().stream()
                .filter(e -> e.getValue().quantidade().signum() > 0)
                .filter(e -> disponivel(estoques.get(e.getKey())).compareTo(e.getValue().quantidade()) < 0)
                .map(e -> {
                    Ingrediente ingrediente = e.getValue().ingrediente();
                    return "Estoque insuficiente de %s: disponível %s %s, necessário %s %s".formatted(
                            ingrediente.getNome(),
                            formatar(disponivel(estoques.get(e.getKey()))), ingrediente.getUnidadeMedida(),
                            formatar(e.getValue().quantidade()), ingrediente.getUnidadeMedida());
                })
                .toList();

        if (!faltas.isEmpty()) {
            throw new RegraNegocioException(String.join("; ", faltas));
        }
    }

    private record Necessidade(Ingrediente ingrediente, BigDecimal quantidade) {
    }

    @Transactional
    public void excluir(Long id) {
        Estoque estoque = buscarEntidade(id);
        Long idIngrediente = estoque.getIngrediente().getId();

        if (produtoIngredienteRepository.existsByIngredienteIdAndProdutoFlativo(idIngrediente, "S")) {
            throw new RegraNegocioException(
                    "Ingrediente em uso por produto ativo. Não é possível excluir o estoque.");
        }

        estoqueRepository.delete(estoque);
    }

    // estoque inexistente, ou de ingrediente inativo, vira 404
    private Estoque buscarEntidade(Long id) {
        return estoqueRepository.findById(id)
                .filter(e -> "S".equals(e.getIngrediente().getFlativo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque", id));
    }

    private BigDecimal disponivel(Estoque estoque) {
        return estoque == null ? BigDecimal.ZERO : estoque.getQtdAtual();
    }

    // 1.500 -> "1.5", 200.000 -> "200"
    private String formatar(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

}