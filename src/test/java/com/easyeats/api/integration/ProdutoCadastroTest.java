package com.easyeats.api.integration;

import com.easyeats.api.entity.Categoria;
import com.easyeats.api.entity.Preco;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro de Produto: grava o produto, vincula à Categoria e cria o Preco ativo na mesma transação.
 *
 * Nota: o ProdutoMapper lança IllegalArgumentException para categoria inexistente, tratada pelo
 * GlobalExceptionHandler como HTTP 422.
 */
@DisplayName("Cadastro de Produto")
class ProdutoCadastroTest extends IntegrationTestBase {

    private static String json(String nome, Long categoriaId, String preco) {
        return """
                {"nome": %s, "descricao": "Descrição", "categoriaId": %s, "preco": %s}
                """.formatted(nome == null ? "null" : "\"" + nome + "\"", categoriaId, preco);
    }

    // ------------------------------------------------------------------ sucesso

    @Test
    @DisplayName("cadastro válido: 201, produto vinculado à categoria e preço ativo criado")
    void cadastroValido() throws Exception {
        Categoria categoria = criarCategoria("Lanches");

        var resposta = post("/produto", json("X-Burger", categoria.getId(), "25.90"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("X-Burger"))
                .andExpect(jsonPath("$.flativo").value("S"))
                .andExpect(jsonPath("$.categoriaId").value(categoria.getId()))
                .andExpect(jsonPath("$.preco").value(25.9));

        Long produtoId = idDaResposta(resposta);

        Produto produto = produtoRepository.findById(produtoId).orElseThrow();
        assertThat(produto.getNome()).isEqualTo("X-Burger");
        assertThat(produto.getFlativo()).isEqualTo("S");
        assertThat(produto.getDataCriacao()).isNotNull();

        List<Preco> precos = precoRepository.findByProdutoId(produtoId);
        assertThat(precos).hasSize(1);
        assertThat(precos.get(0).getValor()).isEqualByComparingTo("25.90");
        assertThat(precos.get(0).getFlativo()).isEqualTo("S");

        // a consulta devolve o vínculo com a categoria e o preço ativo
        get("/produto/" + produtoId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaId").value(categoria.getId()))
                .andExpect(jsonPath("$.preco").value(25.9));
    }

    @Test
    @DisplayName("produto é sempre criado ativo, mesmo que o cliente envie flativo = N")
    void sempreCriadoAtivo() throws Exception {
        Categoria categoria = criarCategoria("Lanches");

        var resposta = post("/produto", """
                {"nome": "Hot Dog", "categoriaId": %d, "preco": 10.00, "flativo": "N"}
                """.formatted(categoria.getId()))
                .andExpect(status().isCreated());

        assertThat(produtoRepository.findById(idDaResposta(resposta)).orElseThrow().getFlativo())
                .isEqualTo("S");
    }

    @Test
    @DisplayName("vários produtos na mesma categoria são permitidos")
    void variosProdutosNaMesmaCategoria() throws Exception {
        Categoria categoria = criarCategoria("Lanches");

        post("/produto", json("X-Burger", categoria.getId(), "25.90")).andExpect(status().isCreated());
        post("/produto", json("X-Salada", categoria.getId(), "27.90")).andExpect(status().isCreated());

        assertThat(produtoRepository.count()).isEqualTo(2);
        assertThat(precoRepository.count()).isEqualTo(2);
    }

    // ------------------------------------------------------------------ validações

    static Stream<Arguments> cadastrosInvalidos() {
        return Stream.of(
                Arguments.of("nome nulo", json(null, 1L, "10.00"),
                        "nome: O nome é obrigatório"),
                Arguments.of("nome em branco", json("  ", 1L, "10.00"),
                        "nome: O nome é obrigatório"),
                Arguments.of("categoria ausente", json("Pizza", null, "10.00"),
                        "categoriaId: A categoria é obrigatória"),
                Arguments.of("preço ausente", json("Pizza", 1L, "null"),
                        "preco: O preço é obrigatório"),
                Arguments.of("preço zero", json("Pizza", 1L, "0"),
                        "preco: O preço deve ser maior que zero"),
                Arguments.of("preço negativo", json("Pizza", 1L, "-5.00"),
                        "preco: O preço deve ser maior que zero")
        );
    }

    @ParameterizedTest(name = "{0} -> 400 e nada é gravado")
    @MethodSource("cadastrosInvalidos")
    void camposInvalidos(String cenario, String corpo, String campoEsperado) throws Exception {
        criarCategoria("Lanches");

        assertErroValidacao(post("/produto", corpo), campoEsperado);

        assertThat(produtoRepository.count()).isZero();
        assertThat(precoRepository.count()).isZero();
    }

    @Test
    @DisplayName("preço não numérico: 400 e nada é gravado")
    void precoNaoNumerico() throws Exception {
        Categoria categoria = criarCategoria("Lanches");

        post("/produto", json("Pizza", categoria.getId(), "\"caro\""))
                .andExpect(status().isBadRequest());

        assertThat(produtoRepository.count()).isZero();
    }

    // ------------------------------------------------------------------ relacionamento inválido

    @Test
    @DisplayName("categoria inexistente: 422, sem produto nem preço órfãos")
    void categoriaInexistente() throws Exception {
        assertRegraNegocio(
                post("/produto", json("Pizza", ID_INEXISTENTE, "40.00")),
                "Categoria não encontrada: " + ID_INEXISTENTE);

        assertThat(produtoRepository.count()).isZero();
        assertThat(precoRepository.count()).isZero();
    }

    // ------------------------------------------------------------------ consulta / exclusão

    @Test
    @DisplayName("buscar produto inexistente: 404")
    void produtoInexistente() throws Exception {
        get("/produto/" + ID_INEXISTENTE).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("excluir produto: 204, exclusão lógica; some das consultas e o histórico de preço é preservado")
    void excluirProduto() throws Exception {
        Categoria categoria = criarCategoria("Lanches");
        Produto produto = criarProduto("X-Burger", categoria, new BigDecimal("25.90"));

        delete("/produto/" + produto.getId()).andExpect(status().isNoContent());

        assertThat(produtoRepository.findById(produto.getId()).orElseThrow().getFlativo()).isEqualTo("N");
        assertThat(precoRepository.findByProdutoId(produto.getId())).hasSize(1);

        get("/produto/" + produto.getId()).andExpect(status().isNotFound());
        get("/produto").andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    // ------------------------------------------------------------------ segurança

    @Test
    @DisplayName("requisição sem token é barrada e nada é gravado")
    void semToken() throws Exception {
        Categoria categoria = criarCategoria("Lanches");

        assertBarradoPelaSeguranca(postSemToken("/produto", json("Pizza", categoria.getId(), "40.00")));

        assertThat(produtoRepository.count()).isZero();
    }
}
