package com.easyeats.api.service;

import com.easyeats.api.dto.PrecoDto;
import com.easyeats.api.dto.mapper.PrecoMapper;
import com.easyeats.api.entity.Preco;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.repository.PrecoRepository;
import com.easyeats.api.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrecoService {

    private final PrecoRepository repository;

    private final ProdutoRepository produtoRepository;

    private final PrecoMapper mapper;


    @Transactional
    public PrecoDto salvar(PrecoDto dto) {

        Optional<Produto> produto =
                produtoRepository.findById(
                        dto.produtoId()
                );

        if (
                produto.isEmpty()
                ||
                !"S".equals(
                        produto.get().getFlativo()
                )
        ) {

            return null;
        }


        /*
         * Desativa o preço atual.
         */
        Optional<Preco> precoAtual =
                repository
                        .findFirstByProdutoIdAndFlativoOrderByIdDesc(
                                dto.produtoId(),
                                "S"
                        );

        if (precoAtual.isPresent()) {

            Preco antigo =
                    precoAtual.get();

            antigo.setFlativo("N");

            repository.save(antigo);
        }


        /*
         * Cria novo preço.
         */
        Preco preco =
                mapper.toEntity(
                        dto,
                        produto.get()
                );

        preco.setFlativo("S");

        return mapper.toDto(
                repository.save(preco)
        );
    }


    public List<PrecoDto> listarTodos() {

        return repository.findAll()
                .stream()

                .filter(preco ->
                        "S".equals(
                                preco.getFlativo()
                        )
                )

                .map(mapper::toDto)

                .toList();
    }


    public Optional<PrecoDto> buscarPorId(
            Long id
    ) {

        Optional<Preco> preco =
                repository.findById(id);

        if (
                preco.isEmpty()
                ||
                !"S".equals(
                        preco.get().getFlativo()
                )
        ) {

            return Optional.empty();
        }

        return Optional.of(
                mapper.toDto(
                        preco.get()
                )
        );
    }


    public List<PrecoDto> listarPorProduto(
            Long produtoId
    ) {

        return repository
                .findByProdutoId(produtoId)

                .stream()

                .filter(preco ->
                        "S".equals(
                                preco.getFlativo()
                        )
                )

                .map(mapper::toDto)

                .toList();
    }


    public void excluir(Long id) {

        Optional<Preco> preco =
                repository.findById(id);

        if (preco.isPresent()) {

            preco.get()
                    .setFlativo("N");

            repository.save(
                    preco.get()
            );
        }
    }
}