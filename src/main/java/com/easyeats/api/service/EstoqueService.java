package com.easyeats.api.service;

import com.easyeats.api.dto.EstoqueDto;
import com.easyeats.api.dto.mapper.EstoqueMapper;
import com.easyeats.api.entity.Estoque;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.exception.RecursoNaoEncontradoException;
import com.easyeats.api.exception.RegraNegocioException;
import com.easyeats.api.repository.EstoqueRepository;
import com.easyeats.api.repository.IngredienteRepository;
import com.easyeats.api.repository.ProdutoIngredienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        if (!estoque.getQtdAtual().equals(dto.qtdAtual())) {
            throw new RegraNegocioException(
                    "A quantidade atual só pode ser alterada por entrada ou saída de estoque");
        }

        estoque.setQtdMinima(dto.qtdMinima());

        return mapper.toDto(estoqueRepository.save(estoque));
    }

    @Transactional
    public EstoqueDto registrarEntrada(Long id, int quantidade) {
        Estoque estoque = buscarEntidade(id);

        estoque.setQtdAtual(estoque.getQtdAtual() + quantidade);

        return mapper.toDto(estoqueRepository.save(estoque));
    }

    @Transactional
    public EstoqueDto registrarSaida(Long id, int quantidade) {
        Estoque estoque = buscarEntidade(id);

        if (estoque.getQtdAtual() < quantidade) {
            throw new RegraNegocioException("Estoque insuficiente de " + estoque.getIngrediente().getNome()
                    + ": disponível " + estoque.getQtdAtual() + ", solicitado " + quantidade);
        }

        estoque.setQtdAtual(estoque.getQtdAtual() - quantidade);

        return mapper.toDto(estoqueRepository.save(estoque));
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
}