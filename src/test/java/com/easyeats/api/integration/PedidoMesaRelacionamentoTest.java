package com.easyeats.api.integration;

import com.easyeats.api.entity.Categoria;
import com.easyeats.api.entity.Mesa;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Relacionamento da Mesa com o restante do domínio: todo Pedido pertence a uma Mesa ativa e seus itens
 * apontam para Produtos ativos que tenham preço ativo (o valor do item vem do preço, não do cliente).
 *
 * Nota: o PedidoMapper lança IllegalArgumentException para mesa ausente/inexistente/inativa, tratada pelo
 * GlobalExceptionHandler como HTTP 422. Já os erros de produto/preço do ItemPedidoService são
 * ResponseStatusException (HTTP 400).
 */
@DisplayName("Relacionamento Mesa x Pedido x Produto")
class PedidoMesaRelacionamentoTest extends IntegrationTestBase {

    private Mesa mesa;
    private Produto produto;

    @BeforeEach
    void criarMassaBase() {
        Categoria categoria = criarCategoria("Lanches");
        mesa = criarMesa(5);
        produto = criarProduto("X-Burger", categoria, new BigDecimal("25.90"));
    }

    private static String pedido(Object mesaId, Object produtoId, Object quantidade) {
        return """
                {"mesaId": %s, "cliente": "João", "itens": [{"produtoId": %s, "quantidade": %s}]}
                """.formatted(mesaId, produtoId, quantidade);
    }

    // ------------------------------------------------------------------ sucesso

    @Test
    @DisplayName("pedido válido: 201, vinculado à mesa, valor do item vem do preço ativo e total calculado")
    void pedidoValido() throws Exception {
        var resposta = post("/pedido", pedido(mesa.getId(), produto.getId(), 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mesaId").value(mesa.getId()))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].produtoId").value(produto.getId()))
                .andExpect(jsonPath("$.itens[0].valorUnitario").value(25.9))
                .andExpect(jsonPath("$.valorTotal").value(51.8));

        assertThat(pedidoRepository.count()).isEqualTo(1);

        // o que foi gravado: consulta o pedido de volta e confere o vínculo com a mesa
        get("/pedido/" + idDaResposta(resposta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesaId").value(mesa.getId()))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2));
    }

    // ------------------------------------------------------------------ relacionamentos inválidos (mesa)

    @Test
    @DisplayName("mesa inexistente: 422 e nada é gravado")
    void mesaInexistente() throws Exception {
        assertRegraNegocio(
                post("/pedido", pedido(ID_INEXISTENTE, produto.getId(), 1)),
                "Mesa não encontrada ou inativa: " + ID_INEXISTENTE);

        assertThat(pedidoRepository.count()).isZero();
    }

    @Test
    @DisplayName("mesa inativa (excluída): 422 e nada é gravado")
    void mesaInativa() throws Exception {
        delete("/mesa/" + mesa.getId()).andExpect(status().isNoContent());

        assertRegraNegocio(
                post("/pedido", pedido(mesa.getId(), produto.getId(), 1)),
                "Mesa não encontrada ou inativa: " + mesa.getId());

        assertThat(pedidoRepository.count()).isZero();
    }

    @Test
    @DisplayName("mesa não informada: 422 e nada é gravado")
    void mesaNaoInformada() throws Exception {
        assertRegraNegocio(
                post("/pedido", pedido(null, produto.getId(), 1)),
                "A mesa é obrigatória");

        assertThat(pedidoRepository.count()).isZero();
    }

    // ------------------------------------------------------------------ relacionamentos inválidos (itens)

    @Test
    @DisplayName("pedido sem itens: 400")
    void semItens() throws Exception {
        assertErroValidacao(
                post("/pedido", "{\"mesaId\": %d, \"itens\": []}".formatted(mesa.getId())),
                "itens: O pedido deve ter ao menos um item");

        assertThat(pedidoRepository.count()).isZero();
    }

    @Test
    @DisplayName("quantidade zero ou ausente no item: 400")
    void quantidadeInvalida() throws Exception {
        assertErroValidacao(
                post("/pedido", pedido(mesa.getId(), produto.getId(), 0)),
                "itens[0].quantidade: A quantidade deve ser maior que zero");
        assertErroValidacao(
                post("/pedido", pedido(mesa.getId(), produto.getId(), null)),
                "itens[0].quantidade: A quantidade é obrigatória");

        assertThat(pedidoRepository.count()).isZero();
    }

    @Test
    @DisplayName("produto inexistente, inativo ou sem preço ativo: 400 e nada é gravado")
    void produtoInvalidoNoItem() throws Exception {
        Produto inativo = criarProdutoInativo("Antigo", produto.getCategoria(), new BigDecimal("9.90"));
        Produto semPreco = criarProduto("Sem preço", produto.getCategoria(), null);

        post("/pedido", pedido(mesa.getId(), ID_INEXISTENTE, 1)).andExpect(status().isBadRequest());
        post("/pedido", pedido(mesa.getId(), inativo.getId(), 1)).andExpect(status().isBadRequest());
        post("/pedido", pedido(mesa.getId(), semPreco.getId(), 1)).andExpect(status().isBadRequest());

        assertThat(pedidoRepository.count()).isZero();
    }
}
