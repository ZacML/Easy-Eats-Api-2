package com.easyeats.api.integration;

import com.easyeats.api.entity.Categoria;
import com.easyeats.api.entity.Estoque;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.entity.Mesa;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Massa: X-Burger = 1 pão + 150 carne + 30 queijo; X-Salada = 1 pão + 150 carne + 40 tomate
 * (o estoque é inteiro, então as fichas usam a mesma unidade base do estoque).
 */
@DisplayName("Estoque x Pedido (validação na inclusão e baixa em EM_PREPARO)")
class PedidoEstoqueTest extends IntegrationTestBase {

    private Mesa mesa;
    private Produto xBurger;
    private Produto xSalada;
    private Ingrediente pao;
    private Ingrediente carne;
    private Ingrediente queijo;
    private Ingrediente tomate;

    @BeforeEach
    void criarMassaBase() {
        Categoria categoria = criarCategoria("Lanches");
        mesa = criarMesa(1);

        pao = criarIngrediente("Pão");
        carne = criarIngrediente("Carne");
        queijo = criarIngrediente("Queijo");
        tomate = criarIngrediente("Tomate");

        criarEstoque(pao, 100);
        criarEstoque(carne, 10_000);
        criarEstoque(queijo, 5_000);
        criarEstoque(tomate, 5_000);

        xBurger = criarProduto("X-Burger", categoria, new BigDecimal("25.90"));
        vincular(xBurger, pao, 1);
        vincular(xBurger, carne, 150);
        vincular(xBurger, queijo, 30);

        xSalada = criarProduto("X-Salada", categoria, new BigDecimal("27.90"));
        vincular(xSalada, pao, 1);
        vincular(xSalada, carne, 150);
        vincular(xSalada, tomate, 40);
    }

    // ------------------------------------------------------------------ helpers

    private static String item(Produto produto, int quantidade) {
        return "{\"produtoId\": %d, \"quantidade\": %d}".formatted(produto.getId(), quantidade);
    }

    private String corpoPedido(String... itens) {
        return "{\"mesaId\": %d, \"cliente\": \"João\", \"itens\": [%s]}"
                .formatted(mesa.getId(), String.join(",", itens));
    }

    private Long criarPedido(String... itens) throws Exception {
        return idDaResposta(post("/pedido", corpoPedido(itens)).andExpect(status().isCreated()));
    }

    private ResultActions atualizarStatus(Long pedidoId, String status) throws Exception {
        return patch("/pedido/" + pedidoId + "/status/" + status);
    }

    private BigDecimal estoqueDe(Ingrediente ingrediente) {
        return estoqueRepository.findByIngredienteId(ingrediente.getId()).orElseThrow().getQtdAtual();
    }

    private void definirEstoque(Ingrediente ingrediente, BigDecimal quantidade) {
        Estoque estoque = estoqueRepository.findByIngredienteId(ingrediente.getId()).orElseThrow();
        estoque.setQtdAtual(quantidade);
        estoqueRepository.save(estoque);
    }

    private boolean baixado(Long pedidoId) {
        return pedidoRepository.findById(pedidoId).orElseThrow().isEstoqueBaixado();
    }

    // ------------------------------------------------------------------ 1) estoque suficiente

    @Test
    @DisplayName("ABERTO -> EM_PREPARO com estoque suficiente: status muda e os ingredientes são baixados")
    void emPreparoComEstoqueSuficiente() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 2));

        // incluir o item apenas valida: nada é baixado
        assertThat(estoqueDe(pao)).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(estoqueDe(carne)).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(estoqueDe(queijo)).isEqualByComparingTo(new BigDecimal("5000"));

        atualizarStatus(pedidoId, "EM_PREPARO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PREPARO"));

        assertThat(estoqueDe(pao)).isEqualByComparingTo(new BigDecimal("98"));
        assertThat(estoqueDe(carne)).isEqualByComparingTo(new BigDecimal("9700"));
        assertThat(estoqueDe(queijo)).isEqualByComparingTo(new BigDecimal("4940"));
        assertThat(baixado(pedidoId)).isTrue();
    }

    // ------------------------------------------------------------------ 2) vários produtos / ingredientes

    @Test
    @DisplayName("pedido com 2 produtos: ingredientes em comum são somados e os demais baixados separadamente")
    void pedidoComVariosProdutos() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 1), item(xSalada, 2));

        atualizarStatus(pedidoId, "EM_PREPARO").andExpect(status().isOk());

        assertThat(estoqueDe(pao)).isEqualByComparingTo(BigDecimal.valueOf(100 - 1 - 2));
        assertThat(estoqueDe(carne)).isEqualByComparingTo(BigDecimal.valueOf(10_000 - 450));
        assertThat(estoqueDe(queijo)).isEqualByComparingTo(BigDecimal.valueOf(5_000 - 30));
        assertThat(estoqueDe(tomate)).isEqualByComparingTo(BigDecimal.valueOf(5_000 - 80));
    }

    // ------------------------------------------------------------------ 3) consumo proporcional

    @ParameterizedTest(name = "{0} unidade(s) de X-Burger consomem {0}x a ficha técnica")
    @ValueSource(ints = {1, 3, 7})
    @DisplayName("o consumo é proporcional à quantidade pedida")
    void consumoProporcional(int quantidade) throws Exception {
        Long pedidoId = criarPedido(item(xBurger, quantidade));

        atualizarStatus(pedidoId, "EM_PREPARO").andExpect(status().isOk());

        assertThat(estoqueDe(pao)).isEqualByComparingTo(BigDecimal.valueOf(100 - quantidade));
        assertThat(estoqueDe(carne)).isEqualByComparingTo(BigDecimal.valueOf(10_000 - 150 * quantidade));
        assertThat(estoqueDe(queijo)).isEqualByComparingTo(BigDecimal.valueOf(5_000 - 30 * quantidade));
    }

    @Test
    @DisplayName("quantidade fracionada na ficha técnica: baixa exatamente o consumo, sem arredondar")
    void consumoFracionado() throws Exception {
        Ingrediente sal = criarIngrediente("Sal");
        criarEstoque(sal, new BigDecimal("10"));
        Produto fritas = criarProduto("Fritas", xBurger.getCategoria(), new BigDecimal("12.00"));
        vincular(fritas, sal, 0.15);

        Long pedidoId = criarPedido(item(fritas, 2));   // 0,15 x 2 = 0,30

        atualizarStatus(pedidoId, "EM_PREPARO").andExpect(status().isOk());

        assertThat(estoqueDe(sal)).isEqualByComparingTo("9.70");
    }

    // ------------------------------------------------------------------ 4) estoque insuficiente

    @Test
    @DisplayName("ao incluir o item sem estoque suficiente: 422 informando ingrediente, disponível e necessário")
    void inclusaoSemEstoque() throws Exception {
        definirEstoque(carne, new BigDecimal("200"));

        assertRegraNegocio(
                post("/pedido", corpoPedido(item(xBurger, 2))),
                "Estoque insuficiente de Carne: disponível 200 kg, necessário 300 kg");

        assertThat(pedidoRepository.count()).isZero();
        assertThat(estoqueDe(carne)).isEqualByComparingTo(new BigDecimal("200"));
    }

    @Test
    @DisplayName("ao adicionar item a um pedido existente sem estoque: 422 e o item não é gravado")
    void adicionarItemSemEstoque() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 1));
        definirEstoque(carne, new BigDecimal("100"));

        assertRegraNegocio(
                post("/item-pedido/pedido/" + pedidoId, item(xSalada, 1)),
                "Estoque insuficiente de Carne: disponível 100 kg, necessário 150 kg");

        get("/item-pedido/pedido/" + pedidoId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("estoque acabou depois de o pedido ser criado: EM_PREPARO é barrado (422) e o status continua ABERTO")
    void emPreparoSemEstoque() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 1));
        definirEstoque(carne, new BigDecimal("100"));

        assertRegraNegocio(
                atualizarStatus(pedidoId, "EM_PREPARO"),
                "Estoque insuficiente de Carne: disponível 100 kg, necessário 150 kg");

        get("/pedido/" + pedidoId).andExpect(jsonPath("$.status").value("ABERTO"));
        assertThat(baixado(pedidoId)).isFalse();
    }

    @Test
    @DisplayName("ingrediente sem estoque cadastrado conta como disponível 0")
    void ingredienteSemEstoqueCadastrado() throws Exception {
        Ingrediente sal = criarIngrediente("Sal");
        Produto fritas = criarProduto("Fritas", xBurger.getCategoria(), new BigDecimal("12.00"));
        vincular(fritas, sal, 1);

        assertRegraNegocio(
                post("/pedido", corpoPedido(item(fritas, 1))),
                "Estoque insuficiente de Sal: disponível 0 kg, necessário 1 kg");
    }

    // ------------------------------------------------------------------ 5) rollback

    @Test
    @DisplayName("falta de um único ingrediente: nenhum outro ingrediente é baixado e o pedido não muda")
    void rollbackQuandoFaltaUmIngrediente() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 2));   // precisa de 60 de queijo
        definirEstoque(queijo, new BigDecimal("50"));               // pão e carne continuam suficientes

        assertRegraNegocio(
                atualizarStatus(pedidoId, "EM_PREPARO"),
                "Estoque insuficiente de Queijo: disponível 50 kg, necessário 60 kg");

        assertThat(estoqueDe(pao)).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(estoqueDe(carne)).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(estoqueDe(queijo)).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(baixado(pedidoId)).isFalse();
        get("/pedido/" + pedidoId).andExpect(jsonPath("$.status").value("ABERTO"));
    }

    // ------------------------------------------------------------------ 6) baixa duplicada

    @Test
    @DisplayName("repetir EM_PREPARO não baixa de novo")
    void repetirEmPreparo() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 2));

        atualizarStatus(pedidoId, "EM_PREPARO").andExpect(status().isOk());
        atualizarStatus(pedidoId, "EM_PREPARO").andExpect(status().isOk());

        assertThat(estoqueDe(pao)).isEqualByComparingTo(new BigDecimal("98"));
        assertThat(estoqueDe(carne)).isEqualByComparingTo(new BigDecimal("9700"));
        assertThat(estoqueDe(queijo)).isEqualByComparingTo(new BigDecimal("4940"));
    }

    @Test
    @DisplayName("voltar para ABERTO e entrar de novo em EM_PREPARO não baixa de novo")
    void voltarEEntrarDeNovoEmPreparo() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 2));

        atualizarStatus(pedidoId, "EM_PREPARO").andExpect(status().isOk());
        atualizarStatus(pedidoId, "ABERTO").andExpect(status().isOk());
        atualizarStatus(pedidoId, "EM_PREPARO").andExpect(status().isOk());

        assertThat(estoqueDe(carne)).isEqualByComparingTo(new BigDecimal("9700"));
        assertThat(baixado(pedidoId)).isTrue();
    }

    @Test
    @DisplayName("PUT do pedido com status EM_PREPARO também baixa (uma vez só)")
    void baixaViaPutDoPedido() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 1));
        String corpo = "{\"mesaId\": %d, \"cliente\": \"João\", \"status\": \"EM_PREPARO\", \"itens\": [%s]}"
                .formatted(mesa.getId(), item(xBurger, 1));

        put("/pedido/" + pedidoId, corpo).andExpect(status().isOk());
        put("/pedido/" + pedidoId, corpo).andExpect(status().isOk());

        assertThat(estoqueDe(carne)).isEqualByComparingTo(BigDecimal.valueOf(10_000 - 150));
        assertThat(estoqueDe(pao)).isEqualByComparingTo(new BigDecimal ("99"));
    }

    // ------------------------------------------------------------------ 7) status sem movimentação

    @Test
    @DisplayName("ABERTO -> CANCELADO não movimenta o estoque")
    void cancelarNaoMovimentaEstoque() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 2));

        atualizarStatus(pedidoId, "CANCELADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        assertThat(estoqueDe(pao)).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(estoqueDe(carne)).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(estoqueDe(queijo)).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(baixado(pedidoId)).isFalse();
    }

    @Test
    @DisplayName("EM_PREPARO -> FINALIZADO não baixa de novo")
    void finalizarNaoMovimentaEstoque() throws Exception {
        Long pedidoId = criarPedido(item(xBurger, 2));

        atualizarStatus(pedidoId, "EM_PREPARO").andExpect(status().isOk());
        atualizarStatus(pedidoId, "FINALIZADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));

        assertThat(estoqueDe(pao)).isEqualByComparingTo(new BigDecimal("98"));
        assertThat(estoqueDe(carne)).isEqualByComparingTo(new BigDecimal("9700"));
        assertThat(estoqueDe(queijo)).isEqualByComparingTo(new BigDecimal("4940"));
    }
}