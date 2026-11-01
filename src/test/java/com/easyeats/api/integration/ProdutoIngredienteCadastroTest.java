package com.easyeats.api.integration;

import com.easyeats.api.entity.Categoria;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.entity.ProdutoIngrediente;
import com.easyeats.api.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro de ProdutoIngrediente em lote: um produto recebe vários ingredientes (cada um com a sua
 * quantidade) em uma única requisição, de forma "tudo ou nada".
 */
@DisplayName("Cadastro de ProdutoIngrediente (produto com múltiplos ingredientes)")
class ProdutoIngredienteCadastroTest extends IntegrationTestBase {

    private Produto hamburguer;
    private Ingrediente pao;
    private Ingrediente carne;
    private Ingrediente queijo;

    @BeforeEach
    void criarMassaBase() {
        Categoria categoria = criarCategoria("Lanches");
        hamburguer = criarProduto("X-Burger", categoria, new BigDecimal("25.90"));
        pao = criarIngrediente("Pão");
        carne = criarIngrediente("Carne");
        queijo = criarIngrediente("Queijo");
    }

    // ------------------------------------------------------------------ helpers

    private static String item(Object ingredienteId, Object quantidade) {
        return "{\"ingredienteId\": %s, \"quantidade\": %s}".formatted(ingredienteId, quantidade);
    }

    private static String lote(Object produtoId, String... itens) {
        return "{\"produtoId\": %s, \"ingredientes\": [%s]}".formatted(produtoId, String.join(",", itens));
    }

    /** ingredienteId -> quantidade, lido direto da tabela. */
    private Map<Long, Double> vinculosDoProduto(Long produtoId) {
        return produtoIngredienteRepository.findAll().stream()
                .filter(v -> v.getId().getProdutoId().equals(produtoId))
                .collect(Collectors.toMap(v -> v.getId().getIngredienteId(), ProdutoIngrediente::getQuantidade));
    }

    // ------------------------------------------------------------------ sucesso

    @Test
    @DisplayName("3 ingredientes em uma requisição: 201, resposta com a lista e 3 vínculos gravados com suas quantidades")
    void cadastroEmLote() throws Exception {
        post("/produto-ingrediente", lote(hamburguer.getId(),
                item(pao.getId(), 0.1),
                item(carne.getId(), 0.2),
                item(queijo.getId(), 0.05)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.produtoId").value(hamburguer.getId()))
                .andExpect(jsonPath("$.ingredientes.length()").value(3))
                .andExpect(jsonPath("$.ingredientes[0].ingredienteId").value(pao.getId()))
                .andExpect(jsonPath("$.ingredientes[0].quantidade").value(0.1))
                .andExpect(jsonPath("$.ingredientes[2].quantidade").value(0.05));

        assertThat(vinculosDoProduto(hamburguer.getId())).containsExactlyInAnyOrderEntriesOf(Map.of(
                pao.getId(), 0.1,
                carne.getId(), 0.2,
                queijo.getId(), 0.05));
    }

    @Test
    @DisplayName("lista com um único ingrediente também é aceita")
    void umIngrediente() throws Exception {
        post("/produto-ingrediente", lote(hamburguer.getId(), item(pao.getId(), 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredientes.length()").value(1));

        assertThat(vinculosDoProduto(hamburguer.getId())).containsOnlyKeys(pao.getId());
    }

    @Test
    @DisplayName("os vínculos ficam só no produto informado e podem ser consultados e removidos individualmente")
    void consultaEExclusaoDeVinculo() throws Exception {
        Produto outro = criarProduto("X-Salada", hamburguer.getCategoria(), new BigDecimal("27.90"));

        post("/produto-ingrediente", lote(hamburguer.getId(),
                item(pao.getId(), 0.1), item(carne.getId(), 0.2)))
                .andExpect(status().isCreated());

        assertThat(vinculosDoProduto(outro.getId())).isEmpty();

        get("/produto-ingrediente/" + hamburguer.getId() + "/" + carne.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(0.2));

        delete("/produto-ingrediente/" + hamburguer.getId() + "/" + pao.getId())
                .andExpect(status().isNoContent());

        assertThat(vinculosDoProduto(hamburguer.getId())).containsOnlyKeys(carne.getId());
    }

    @Test
    @DisplayName("novo lote com ingredientes diferentes acrescenta aos já vinculados")
    void loteIncremental() throws Exception {
        post("/produto-ingrediente", lote(hamburguer.getId(), item(pao.getId(), 0.1)))
                .andExpect(status().isCreated());
        post("/produto-ingrediente", lote(hamburguer.getId(), item(carne.getId(), 0.2), item(queijo.getId(), 0.05)))
                .andExpect(status().isCreated());

        assertThat(vinculosDoProduto(hamburguer.getId()))
                .containsOnlyKeys(pao.getId(), carne.getId(), queijo.getId());
    }

    // ------------------------------------------------------------------ validações do corpo

    @Test
    @DisplayName("lista vazia: 400")
    void listaVazia() throws Exception {
        assertErroValidacao(
                post("/produto-ingrediente", lote(hamburguer.getId())),
                "ingredientes: Informe ao menos um ingrediente");

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    @Test
    @DisplayName("lista ausente: 400")
    void listaAusente() throws Exception {
        assertErroValidacao(
                post("/produto-ingrediente", "{\"produtoId\": " + hamburguer.getId() + "}"),
                "ingredientes: Informe ao menos um ingrediente");

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    @Test
    @DisplayName("produtoId ausente: 400")
    void produtoIdAusente() throws Exception {
        assertErroValidacao(
                post("/produto-ingrediente", lote(null, item(pao.getId(), 0.1))),
                "produtoId: O produto é obrigatório");

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    static Stream<Arguments> segundoItemInvalido() {
        return Stream.of(
                Arguments.of("ingredienteId ausente", item(null, 0.5),
                        "ingredientes[1].ingredienteId: O ingrediente é obrigatório"),
                Arguments.of("quantidade ausente", item(1, null),
                        "ingredientes[1].quantidade: A quantidade é obrigatória"),
                Arguments.of("quantidade zero", item(1, 0),
                        "ingredientes[1].quantidade: A quantidade deve ser maior que zero"),
                Arguments.of("quantidade negativa", item(1, -0.3),
                        "ingredientes[1].quantidade: A quantidade deve ser maior que zero")
        );
    }

    @ParameterizedTest(name = "2º item com {0} -> 400 e NENHUM item é gravado, nem o 1º (válido)")
    @MethodSource("segundoItemInvalido")
    void itemInvalidoNaoGravaNada(String cenario, String itemInvalido, String campoEsperado) throws Exception {
        assertErroValidacao(
                post("/produto-ingrediente", lote(hamburguer.getId(), item(pao.getId(), 0.1), itemInvalido)),
                campoEsperado);

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    @Test
    @DisplayName("quantidade não numérica: 400 e nada é gravado")
    void quantidadeNaoNumerica() throws Exception {
        post("/produto-ingrediente", lote(hamburguer.getId(), item(pao.getId(), "\"muito\"")))
                .andExpect(status().isBadRequest());

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    // ------------------------------------------------------------------ IDs inexistentes / inativos

    @Test
    @DisplayName("produto inexistente: 404")
    void produtoInexistente() throws Exception {
        assertNaoEncontrado(
                post("/produto-ingrediente", lote(ID_INEXISTENTE, item(pao.getId(), 0.1))),
                "Produto não encontrado: " + ID_INEXISTENTE);

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    @Test
    @DisplayName("produto inativo: 404 e nada é gravado")
    void produtoInativo() throws Exception {
        Produto inativo = criarProdutoInativo("Antigo", hamburguer.getCategoria(), new BigDecimal("9.90"));

        assertNaoEncontrado(
                post("/produto-ingrediente", lote(inativo.getId(), item(pao.getId(), 0.1))),
                "Produto não encontrado: " + inativo.getId());

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    @Test
    @DisplayName("um ingrediente inexistente no meio da lista: 404 e NENHUM vínculo é gravado")
    void ingredienteInexistenteNaoGravaNada() throws Exception {
        assertNaoEncontrado(
                post("/produto-ingrediente", lote(hamburguer.getId(),
                        item(pao.getId(), 0.1),
                        item(ID_INEXISTENTE, 0.2),
                        item(queijo.getId(), 0.05))),
                "Ingrediente não encontrado: " + ID_INEXISTENTE);

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    @Test
    @DisplayName("ingrediente inativo na lista: 404 e NENHUM vínculo é gravado")
    void ingredienteInativoNaoGravaNada() throws Exception {
        Ingrediente inativo = criarIngredienteInativo("Descontinuado");

        assertNaoEncontrado(
                post("/produto-ingrediente", lote(hamburguer.getId(),
                        item(pao.getId(), 0.1),
                        item(inativo.getId(), 0.2))),
                "Ingrediente não encontrado: " + inativo.getId());

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    // ------------------------------------------------------------------ duplicidade

    @Test
    @DisplayName("mesmo ingrediente repetido na lista: 422 e nada é gravado")
    void ingredienteRepetidoNaLista() throws Exception {
        assertRegraNegocio(
                post("/produto-ingrediente", lote(hamburguer.getId(),
                        item(pao.getId(), 0.1),
                        item(carne.getId(), 0.2),
                        item(pao.getId(), 0.3))),
                "Ingrediente repetido na lista: " + pao.getId());

        assertThat(produtoIngredienteRepository.count()).isZero();
    }

    @Test
    @DisplayName("ingrediente já vinculado ao produto: 422, quantidade original preservada e o restante do lote não é gravado")
    void ingredienteJaVinculado() throws Exception {
        vincular(hamburguer, pao, 0.1);

        assertRegraNegocio(
                post("/produto-ingrediente", lote(hamburguer.getId(),
                        item(carne.getId(), 0.2),
                        item(pao.getId(), 9.9))),
                "O ingrediente " + pao.getId() + " já está vinculado ao produto");

        assertThat(vinculosDoProduto(hamburguer.getId()))
                .containsExactlyInAnyOrderEntriesOf(Map.of(pao.getId(), 0.1));
    }

    // ------------------------------------------------------------------ segurança

    @Test
    @DisplayName("requisição sem token é barrada e nada é gravado")
    void semToken() throws Exception {
        assertBarradoPelaSeguranca(
                postSemToken("/produto-ingrediente", lote(hamburguer.getId(), item(pao.getId(), 0.1))));

        assertThat(produtoIngredienteRepository.count()).isZero();
    }
}
