package com.easyeats.api.integration;

import com.easyeats.api.entity.Categoria;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Cadastro de Ingrediente")
class IngredienteCadastroTest extends IntegrationTestBase {

    private static String json(String nome, String unidade, String custo) {
        return """
                {"nome": %s, "unidadeMedida": %s, "custo": %s}
                """.formatted(texto(nome), texto(unidade), custo);
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    // ------------------------------------------------------------------ sucesso

    @Test
    @DisplayName("cadastro válido: 201, ativo por padrão, nome sem espaços nas pontas e dados persistidos")
    void cadastroValido() throws Exception {
        var resposta = post("/ingredientes", json("  Açúcar  ", "kg", "12.50"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Açúcar"))
                .andExpect(jsonPath("$.unidadeMedida").value("kg"));

        Ingrediente salvo = ingredienteRepository.findById(idDaResposta(resposta)).orElseThrow();
        assertThat(salvo.getNome()).isEqualTo("Açúcar");
        assertThat(salvo.getUnidadeMedida()).isEqualTo("kg");
        assertThat(salvo.getCusto()).isEqualByComparingTo("12.50");
        assertThat(salvo.getFlativo()).isEqualTo("S");
        assertThat(salvo.getDataCriacao()).isNotNull();
    }

    @Test
    @DisplayName("custo zero é aceito (a regra é @PositiveOrZero)")
    void custoZeroEhAceito() throws Exception {
        post("/ingredientes", json("Água", "l", "0"))
                .andExpect(status().isCreated());

        assertThat(ingredienteRepository.count()).isEqualTo(1);
    }

    // ------------------------------------------------------------------ validações

    static Stream<Arguments> cadastrosInvalidos() {
        return Stream.of(
                Arguments.of("nome nulo", json(null, "kg", "1.00"),
                        "nome: O nome do ingrediente é obrigatório"),
                Arguments.of("nome em branco", json("   ", "kg", "1.00"),
                        "nome: O nome do ingrediente é obrigatório"),
                Arguments.of("unidade em branco", json("Sal", "", "1.00"),
                        "unidadeMedida: A unidade de medida é obrigatória"),
                Arguments.of("custo ausente", json("Sal", "kg", "null"),
                        "custo: O custo é obrigatório"),
                Arguments.of("custo negativo", json("Sal", "kg", "-0.01"),
                        "custo: O custo não pode ser negativo")
        );
    }

    @ParameterizedTest(name = "{0} -> 400 e nada é gravado")
    @MethodSource("cadastrosInvalidos")
    void camposInvalidos(String cenario, String corpo, String campoEsperado) throws Exception {
        assertErroValidacao(post("/ingredientes", corpo), campoEsperado);

        assertThat(ingredienteRepository.count()).isZero();
    }

    @Test
    @DisplayName("custo não numérico: 400 e nada é gravado")
    void custoNaoNumerico() throws Exception {
        post("/ingredientes", json("Sal", "kg", "\"muito\""))
                .andExpect(status().isBadRequest());

        assertThat(ingredienteRepository.count()).isZero();
    }

    // ------------------------------------------------------------------ duplicidade

    @Test
    @DisplayName("nome duplicado (ignorando maiúsculas e espaços): 422 e só o original permanece")
    void nomeDuplicado() throws Exception {
        criarIngrediente("Farinha");

        assertRegraNegocio(
                post("/ingredientes", json("  FARINHA ", "kg", "3.00")),
                "Já existe um ingrediente com o nome: FARINHA");

        assertThat(ingredienteRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("alterar para o nome de OUTRO ingrediente: 422 e o registro não muda")
    void alterarParaNomeDeOutro() throws Exception {
        criarIngrediente("Farinha");
        Ingrediente sal = criarIngrediente("Sal");

        assertRegraNegocio(
                put("/ingredientes/" + sal.getId(), json("farinha", "kg", "5.00")),
                "Já existe um ingrediente com o nome: farinha");

        assertThat(ingredienteRepository.findById(sal.getId()).orElseThrow().getNome()).isEqualTo("Sal");
    }

    @Test
    @DisplayName("alterar mantendo o próprio nome é permitido")
    void alterarMantendoOProprioNome() throws Exception {
        Ingrediente sal = criarIngrediente("Sal");

        put("/ingredientes/" + sal.getId(), json("Sal", "g", "9.90"))
                .andExpect(status().isOk());

        Ingrediente alterado = ingredienteRepository.findById(sal.getId()).orElseThrow();
        assertThat(alterado.getUnidadeMedida()).isEqualTo("g");
        assertThat(alterado.getCusto()).isEqualByComparingTo("9.90");
        assertThat(alterado.getFlativo()).isEqualTo("S");
    }

    // ------------------------------------------------------------------ IDs inexistentes / inativos

    @Test
    @DisplayName("buscar, alterar e excluir ingrediente inexistente: 404 com mensagem")
    void ingredienteInexistente() throws Exception {
        String mensagem = "Ingrediente não encontrado: " + ID_INEXISTENTE;

        assertNaoEncontrado(get("/ingredientes/" + ID_INEXISTENTE), mensagem);
        assertNaoEncontrado(put("/ingredientes/" + ID_INEXISTENTE, json("X", "kg", "1.00")), mensagem);
        assertNaoEncontrado(delete("/ingredientes/" + ID_INEXISTENTE), mensagem);
    }

    @Test
    @DisplayName("ingrediente inativo é tratado como inexistente na consulta")
    void ingredienteInativoNaoEhEncontrado() throws Exception {
        Ingrediente inativo = criarIngredienteInativo("Descontinuado");

        assertNaoEncontrado(get("/ingredientes/" + inativo.getId()),
                "Ingrediente não encontrado: " + inativo.getId());
    }

    // ------------------------------------------------------------------ operações bloqueadas

    @Test
    @DisplayName("excluir ingrediente usado por produto ATIVO é bloqueado (422) e ele continua ativo")
    void excluirIngredienteEmUso() throws Exception {
        Categoria categoria = criarCategoria("Lanches");
        Produto produto = criarProduto("X-Burger", categoria, new BigDecimal("25.00"));
        Ingrediente pao = criarIngrediente("Pão");
        vincular(produto, pao, 1.0);

        assertRegraNegocio(
                delete("/ingredientes/" + pao.getId()),
                "Ingrediente em uso por produto ativo. Remova-o do produto antes de excluir.");

        assertThat(ingredienteRepository.findById(pao.getId()).orElseThrow().getFlativo()).isEqualTo("S");
        assertThat(produtoIngredienteRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("excluir ingrediente livre: 204, exclusão lógica (registro permanece com flativo = N)")
    void excluirIngredienteLivre() throws Exception {
        Ingrediente sal = criarIngrediente("Sal");

        delete("/ingredientes/" + sal.getId()).andExpect(status().isNoContent());

        assertThat(ingredienteRepository.findById(sal.getId()).orElseThrow().getFlativo()).isEqualTo("N");
        get("/ingredientes/" + sal.getId()).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ segurança

    @Test
    @DisplayName("requisição sem token é barrada e nada é gravado")
    void semToken() throws Exception {
        assertBarradoPelaSeguranca(postSemToken("/ingredientes", json("Sal", "kg", "1.00")));

        assertThat(ingredienteRepository.count()).isZero();
    }
}
