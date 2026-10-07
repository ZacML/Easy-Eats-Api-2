package com.easyeats.api.service;

import com.easyeats.api.dto.ProdutoDto;
import com.easyeats.api.dto.mapper.ProdutoMapper;
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
public class ProdutoService {

    private final ProdutoRepository repository;
    private final PrecoRepository precoRepository;
    private final ProdutoMapper mapper;


    /*
     * CADASTRAR PRODUTO
     *
     * O preço é obrigatório.
     */
    @Transactional
    public ProdutoDto salvar(ProdutoDto dto) {

        /*
         * Segurança adicional.
         *
         * Mesmo que a validação do DTO falhe,
         * não permitimos produto sem preço.
         */
        if (dto.preco() == null) {

            throw new IllegalArgumentException(
                    "O preço é obrigatório"
            );
        }

        /*
         * Cria o Produto.
         */
        Produto produto = mapper.toEntity(dto);

        produto.setFlativo("S");

        /*
         * Salva primeiro o produto para
         * obter o ID.
         */
        Produto produtoSalvo =
                repository.save(produto);


        /*
         * Cria o preço.
         */
        Preco preco = new Preco();

        preco.setValor(dto.preco());
        preco.setProduto(produtoSalvo);
        preco.setFlativo("S");


        /*
         * Salva o preço.
         */
        precoRepository.save(preco);


        /*
         * Retorna produto + preço.
         */
        return mapper.toDto(
                produtoSalvo,
                preco.getValor()
        );
    }


    /*
     * ALTERAR PRODUTO
     */
    @Transactional
    public ProdutoDto alterar(
            Long id,
            ProdutoDto dto
    ) {

        Optional<Produto> produtoExistente =
                repository.findById(id);

        if (produtoExistente.isEmpty()) {
            return null;
        }

        /*
         * Preço continua obrigatório
         * também na alteração.
         */
        if (dto.preco() == null) {

            throw new IllegalArgumentException(
                    "O preço é obrigatório"
            );
        }

        Produto produtoAtualizado =
                mapper.toEntity(dto);

        produtoAtualizado.setId(id);

        /*
         * Mantém o status atual.
         */
        produtoAtualizado.setFlativo(
                produtoExistente
                        .get()
                        .getFlativo()
        );

        Produto produtoSalvo =
                repository.save(produtoAtualizado);


        /*
         * Desativa o preço atual.
         */
        Optional<Preco> precoAtual =
                precoRepository
                        .findFirstByProdutoIdAndFlativoOrderByIdDesc(
                                id,
                                "S"
                        );

        if (precoAtual.isPresent()) {

            Preco precoAntigo =
                    precoAtual.get();

            precoAntigo.setFlativo("N");
            precoRepository.save(precoAntigo);
        }


        /*
         * Cria o novo preço.
         */
        Preco novoPreco = new Preco();
        novoPreco.setValor(dto.preco());
        novoPreco.setProduto(produtoSalvo);
        novoPreco.setFlativo("S");
        precoRepository.save(novoPreco);


        return mapper.toDto(
                produtoSalvo,
                novoPreco.getValor()
        );
    }


    /*
     * LISTAR PRODUTOS ATIVOS
     */
    @Transactional(readOnly = true)
    public List<ProdutoDto> listarTodos() {

        return repository.findAll()
                .stream()

                .filter(produto ->
                        "S".equals(
                                produto.getFlativo()
                        )
                )

                .map(produto -> {

                    Optional<Preco> preco =
                            precoRepository
                                    .findFirstByProdutoIdAndFlativoOrderByIdDesc(
                                            produto.getId(),
                                            "S"
                                    );

                    return mapper.toDto(
                            produto,
                            preco.map(Preco::getValor)
                                    .orElse(null)
                    );
                })

                .toList();
    }


    /*
     * BUSCAR POR ID
     */
    @Transactional(readOnly = true)
    public Optional<ProdutoDto> buscarPorId(
            Long id
    ) {

        Optional<Produto> produto =
                repository.findById(id);

        if (
                produto.isEmpty()
                ||
                !"S".equals(
                        produto.get().getFlativo()
                )
        ) {

            return Optional.empty();
        }


        Optional<Preco> preco =
                precoRepository
                        .findFirstByProdutoIdAndFlativoOrderByIdDesc(
                                id,
                                "S"
                        );


        return Optional.of(
                mapper.toDto(
                        produto.get(),
                        preco.map(Preco::getValor)
                                .orElse(null)
                )
        );
    }


    /*
     * EXCLUIR PRODUTO
     *
     * Exclusão lógica.
     */
    @Transactional
    public void excluir(Long id) {

        Optional<Produto> produto =
                repository.findById(id);

        if (produto.isPresent()) {

            produto.get()
                    .setFlativo("N");

            repository.save(
                    produto.get()
            );
        }
    }
}