package com.easyeats.api.integration;

import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo ponta a ponta, usando SOMENTE os endpoints (sem fixtures no banco), como o frontend faria:
 * categoria -> ingredientes -> produto -> ingredientes do produto -> mesa -> pedido.
 */
@DisplayName("Fluxo completo de cadastro")
class FluxoCadastroCompletoTest extends IntegrationTestBase {

    @Test
    @DisplayName("cadastra tudo em sequência, confere os vínculos e a integridade ao excluir o produto")
    void fluxoCompleto() throws Exception {
        // 1) categoria
        Long categoriaId = idDaResposta(
                post("/categorias", "{\"nome\": \"Lanches\"}").andExpect(status().isCreated()));

        // 2) ingredientes
        Long paoId = idDaResposta(post("/ingredientes",
                "{\"nome\": \"Pão\", \"unidadeMedida\": \"un\", \"custo\": 1.50}").andExpect(status().isCreated()));
        Long carneId = idDaResposta(post("/ingredientes",
                "{\"nome\": \"Carne\", \"unidadeMedida\": \"kg\", \"custo\": 40.00}").andExpect(status().isCreated()));
        Long queijoId = idDaResposta(post("/ingredientes",
                "{\"nome\": \"Queijo\", \"unidadeMedida\": \"kg\", \"custo\": 30.00}").andExpect(status().isCreated()));

        // 3) produto (já com categoria e preço)
        Long produtoId = idDaResposta(post("/produto", """
                {"nome": "X-Burger", "categoriaId": %d, "preco": 25.90}
                """.formatted(categoriaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaId").value(categoriaId)));

        // 4) ingredientes do produto, em uma única requisição
        post("/produto-ingrediente", """
                {"produtoId": %d, "ingredientes": [
                    {"ingredienteId": %d, "quantidade": 1},
                    {"ingredienteId": %d, "quantidade": 0.15},
                    {"ingredienteId": %d, "quantidade": 0.05}
                ]}
                """.formatted(produtoId, paoId, carneId, queijoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredientes.length()").value(3));

        get("/produto-ingrediente")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // 4.1) estoque dos ingredientes (o pedido só é aceito se houver estoque para a ficha técnica)
        for (Long ingredienteId : new Long[]{paoId, carneId, queijoId}) {
            post("/estoques", "{\"qtdAtual\": 100, \"qtdMinima\": 10, \"idIngrediente\": %d}".formatted(ingredienteId))
                    .andExpect(status().isCreated());
        }

        // 5) mesa e pedido
        Long mesaId = idDaResposta(post("/mesa", "{\"numero\": 1}").andExpect(status().isCreated()));

        post("/pedido", """
                {"mesaId": %d, "cliente": "Maria", "itens": [{"produtoId": %d, "quantidade": 3}]}
                """.formatted(mesaId, produtoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mesaId").value(mesaId))
                .andExpect(jsonPath("$.valorTotal").value(77.7));

        // 6) integridade: ingrediente de produto ativo não pode ser excluído...
        assertRegraNegocio(
                delete("/ingredientes/" + paoId),
                "Ingrediente em uso por produto ativo. Remova-o do produto antes de excluir.");

        // ...mas depois que o produto é excluído (lógico), o ingrediente fica livre
        delete("/produto/" + produtoId).andExpect(status().isNoContent());
        delete("/ingredientes/" + paoId).andExpect(status().isNoContent());

        Ingrediente pao = ingredienteRepository.findById(paoId).orElseThrow();
        assertThat(pao.getFlativo()).isEqualTo("N");

        // os vínculos do produto excluído são preservados (histórico), pois a exclusão é lógica
        assertThat(produtoIngredienteRepository.count()).isEqualTo(3);
    }
}
