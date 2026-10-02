package com.easyeats.api.service;

import com.easyeats.api.dto.IngredienteDto;
import com.easyeats.api.dto.mapper.IngredienteMapper;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.exception.RecursoNaoEncontradoException;
import com.easyeats.api.exception.RegraNegocioException;
import com.easyeats.api.repository.IngredienteRepository;
import com.easyeats.api.repository.ProdutoIngredienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IngredienteService {

    private final IngredienteRepository ingredienteRepository;
    private final ProdutoIngredienteRepository produtoIngredienteRepository;
    private final IngredienteMapper mapper;

    @Transactional
    public IngredienteDto salvar(IngredienteDto dto) {
        String nome = dto.nome().trim();

        if (ingredienteRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe um ingrediente com o nome: " + nome);
        }

        Ingrediente ingrediente = mapper.toEntity(dto);
        ingrediente.setNome(nome);
        ingrediente.setFlativo("S");

        return mapper.toDto(ingredienteRepository.save(ingrediente));
    }

    public List<IngredienteDto> listarTodos() {
        return ingredienteRepository.findByFlativo("S").stream()
                .map(mapper::toDto)
                .toList();
    }

    public IngredienteDto buscarPorId(Long id) {
        return mapper.toDto(buscarAtivo(id));
    }

    public Optional<IngredienteDto> buscarPorNome(String nome) {
        return ingredienteRepository.findByNomeIgnoreCase(nome.trim())
                .filter(i -> "S".equals(i.getFlativo()))
                .map(mapper::toDto);
    }

    @Transactional
    public IngredienteDto alterar(Long id, IngredienteDto dto) {
        Ingrediente existente = buscarAtivo(id);
        String nome = dto.nome().trim();

        if (ingredienteRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("Já existe um ingrediente com o nome: " + nome);
        }

        // altera a entidade que já existe, sem criar outra (preserva flativo e dataCriacao)
        existente.setNome(nome);
        existente.setUnidadeMedida(dto.unidadeMedida());
        existente.setCusto(dto.custo());

        return mapper.toDto(ingredienteRepository.save(existente));
    }

    @Transactional
    public void excluir(Long id) {
        Ingrediente ingrediente = buscarAtivo(id);

        if (produtoIngredienteRepository.existsByIngredienteIdAndProdutoFlativo(id, "S")) {
            throw new RegraNegocioException(
                    "Ingrediente em uso por produto ativo. Remova-o do produto antes de excluir.");
        }

        ingrediente.setFlativo("N");   // exclusão lógica, igual ao Produto
        ingredienteRepository.save(ingrediente);
    }

    // busca só ingrediente ativo; inexistente ou inativo vira 404
    private Ingrediente buscarAtivo(Long id) {
        return ingredienteRepository.findById(id)
                .filter(i -> "S".equals(i.getFlativo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ingrediente", id));
    }
}