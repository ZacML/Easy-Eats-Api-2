package com.easyeats.api.integration;

import com.easyeats.api.entity.Categoria;
import com.easyeats.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Categoria é o agrupamento ao qual todo Produto precisa estar vinculado (o "catálogo" do produto).
 *
 * Nota: IllegalArgumentException (nome duplicado) é tratada pelo GlobalExceptionHandler e vira HTTP 422;
 * categoria inexistente lança RecursoNaoEncontradoException (HTTP 404).
 */
@DisplayName("Cadastro de Categoria")
class CategoriaCadastroTest extends IntegrationTestBase {

    private static String json(String nome) {
        return nome == null ? "{}" : "{\"nome\": \"" + nome + "\"}";
    }

    @Test
    @DisplayName("cadastro válido: 201, ativa por padrão e persistida")
    void cadastroValido() throws Exception {
        var resposta = post("/categorias", json("Bebidas"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Bebidas"))
                .andExpect(jsonPath("$.flativo").value("S"));

        Categoria salva = categoriaRepository.findById(idDaResposta(resposta)).orElseThrow();
        assertThat(salva.getNome()).isEqualTo("Bebidas");
        assertThat(salva.getFlativo()).isEqualTo("S");
        assertThat(salva.getDataCriacao()).isNotNull();
    }

    @ParameterizedTest(name = "nome \"{0}\" -> 400 e nada é gravado")
    @ValueSource(strings = {"", "   "})
    @DisplayName("nome em branco")
    void nomeEmBranco(String nome) throws Exception {
        assertErroValidacao(post("/categorias", json(nome)), "nome: O nome é obrigatório");

        assertThat(categoriaRepository.count()).isZero();
    }

    @Test
    @DisplayName("nome ausente: 400 e nada é gravado")
    void nomeAusente() throws Exception {
        assertErroValidacao(post("/categorias", json(null)), "nome: O nome é obrigatório");

        assertThat(categoriaRepository.count()).isZero();
    }

    @Test
    @DisplayName("nome duplicado (ignorando maiúsculas): 422 e só a original permanece")
    void nomeDuplicado() throws Exception {
        criarCategoria("Sobremesas");

        assertRegraNegocio(post("/categorias", json("SOBREMESAS")),
                "Já existe uma categoria com esse nome");

        assertThat(categoriaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("buscar, alterar e inativar categoria inexistente: 404 com mensagem")
    void categoriaInexistente() throws Exception {
        String mensagem = "Categoria não encontrado: " + ID_INEXISTENTE;

        assertNaoEncontrado(get("/categorias/" + ID_INEXISTENTE), mensagem);
        assertNaoEncontrado(put("/categorias/" + ID_INEXISTENTE, json("Qualquer")), mensagem);
        assertNaoEncontrado(patch("/categorias/" + ID_INEXISTENTE + "/inativar"), mensagem);
    }

    @Test
    @DisplayName("editar para o nome de OUTRA categoria: 422 e o nome original é mantido")
    void editarParaNomeDeOutra() throws Exception {
        criarCategoria("Lanches");
        Categoria bebidas = criarCategoria("Bebidas");

        assertRegraNegocio(put("/categorias/" + bebidas.getId(), json("lanches")),
                "Já existe uma categoria com esse nome");

        assertThat(categoriaRepository.findById(bebidas.getId()).orElseThrow().getNome()).isEqualTo("Bebidas");
    }
}
