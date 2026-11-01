package com.easyeats.api.service;

import com.easyeats.api.dto.IngredienteQuantidadeDto;
import com.easyeats.api.dto.ProdutoIngredienteDto;
import com.easyeats.api.dto.ProdutoIngredienteLoteDto;
import com.easyeats.api.dto.mapper.ProdutoIngredienteMapper;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.entity.ProdutoIngrediente;
import com.easyeats.api.entity.ProdutoIngredienteId;
import com.easyeats.api.exception.RecursoNaoEncontradoException;
import com.easyeats.api.exception.RegraNegocioException;
import com.easyeats.api.repository.IngredienteRepository;
import com.easyeats.api.repository.ProdutoIngredienteRepository;
import com.easyeats.api.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProdutoIngredienteService {

    private final ProdutoIngredienteRepository repository;
    private final ProdutoIngredienteMapper mapper;
    private final ProdutoRepository produtoRepository;
    private final IngredienteRepository ingredienteRepository;

    @Transactional
    public ProdutoIngredienteLoteDto salvar(ProdutoIngredienteLoteDto dto) {

        Produto produto = produtoRepository.findById(dto.produtoId())
                .filter(p -> "S".equals(p.getFlativo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", dto.produtoId()));

        // 1) não permite o mesmo ingrediente repetido na lista
        Set<Long> idsVistos = new HashSet<>();
        for (IngredienteQuantidadeDto item : dto.ingredientes()) {
            if (!idsVistos.add(item.ingredienteId())) {
                throw new RegraNegocioException(
                        "Ingrediente repetido na lista: " + item.ingredienteId());
            }
        }

        // 2) busca todos os ingredientes de uma vez (1 consulta) e mantém só os ativos
        Map<Long, Ingrediente> ingredientes = ingredienteRepository.findAllById(idsVistos)
                .stream()
                .filter(i -> "S".equals(i.getFlativo()))
                .collect(Collectors.toMap(Ingrediente::getId, Function.identity()));

        // 3) valida existência e vínculo prévio ANTES de gravar qualquer coisa
        for (IngredienteQuantidadeDto item : dto.ingredientes()) {
            if (!ingredientes.containsKey(item.ingredienteId())) {
                throw new RecursoNaoEncontradoException("Ingrediente", item.ingredienteId());
            }

            // Remova este bloco se preferir que um vínculo existente seja sobrescrito (upsert)
            if (repository.existsById(
                    new ProdutoIngredienteId(produto.getId(), item.ingredienteId()))) {
                throw new RegraNegocioException(
                        "O ingrediente " + item.ingredienteId() + " já está vinculado ao produto");
            }
        }

        // 4) monta e grava tudo na mesma transação
        List<ProdutoIngrediente> vinculos = dto.ingredientes().stream()
                .map(item -> mapper.toEntity(
                        produto,
                        ingredientes.get(item.ingredienteId()),
                        item.quantidade()))
                .toList();

        return mapper.toLoteDto(produto.getId(), repository.saveAll(vinculos));
    }

    public List<ProdutoIngredienteDto> listar() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public ProdutoIngredienteDto buscarPorId(Long produtoId, Long ingredienteId) {
        ProdutoIngredienteId id =
                new ProdutoIngredienteId(produtoId, ingredienteId);

        ProdutoIngrediente produtoIngrediente = repository.findById(id)
                .orElseThrow();

        return mapper.toDto(produtoIngrediente);
    }

    public void excluir(Long produtoId, Long ingredienteId) {
        ProdutoIngredienteId id =
                new ProdutoIngredienteId(produtoId, ingredienteId);

        repository.deleteById(id);
    }
}