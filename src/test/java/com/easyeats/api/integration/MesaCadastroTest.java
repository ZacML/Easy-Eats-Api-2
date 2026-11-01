package com.easyeats.api.integration;

import com.easyeats.api.entity.Mesa;
import com.easyeats.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro de Mesa.
 *
 * Nota: o MesaService lança IllegalArgumentException para número duplicado, tratada pelo
 * GlobalExceptionHandler como HTTP 422.
 */
@DisplayName("Cadastro de Mesa")
class MesaCadastroTest extends IntegrationTestBase {

    private static String json(Object numero) {
        return "{\"numero\": %s}".formatted(numero);
    }

    // ------------------------------------------------------------------ sucesso

    @Test
    @DisplayName("cadastro válido: 201, mesa ativa e persistida")
    void cadastroValido() throws Exception {
        var resposta = post("/mesa", json(10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.numero").value(10))
                .andExpect(jsonPath("$.flativo").value("S"));

        Mesa salva = mesaRepository.findById(idDaResposta(resposta)).orElseThrow();
        assertThat(salva.getNumero()).isEqualTo(10);
        assertThat(salva.getFlativo()).isEqualTo("S");
    }

    @Test
    @DisplayName("mesa é sempre criada ativa, mesmo que o cliente envie flativo = N")
    void sempreCriadaAtiva() throws Exception {
        var resposta = post("/mesa", "{\"numero\": 3, \"flativo\": \"N\"}")
                .andExpect(status().isCreated());

        assertThat(mesaRepository.findById(idDaResposta(resposta)).orElseThrow().getFlativo()).isEqualTo("S");
    }

    // ------------------------------------------------------------------ validações

    static Stream<Arguments> numerosInvalidos() {
        return Stream.of(
                Arguments.of("número ausente", "{}", "numero: O número da mesa é obrigatório"),
                Arguments.of("número zero", json(0), "numero: O número da mesa deve ser maior que zero"),
                Arguments.of("número negativo", json(-4), "numero: O número da mesa deve ser maior que zero")
        );
    }

    @ParameterizedTest(name = "{0} -> 400 e nada é gravado")
    @MethodSource("numerosInvalidos")
    void numeroInvalido(String cenario, String corpo, String campoEsperado) throws Exception {
        assertErroValidacao(post("/mesa", corpo), campoEsperado);

        assertThat(mesaRepository.count()).isZero();
    }

    @Test
    @DisplayName("número não numérico: 400 e nada é gravado")
    void numeroNaoNumerico() throws Exception {
        post("/mesa", json("\"dez\"")).andExpect(status().isBadRequest());

        assertThat(mesaRepository.count()).isZero();
    }

    // ------------------------------------------------------------------ duplicidade

    @Test
    @DisplayName("número de mesa duplicado: 422 e só a original permanece")
    void numeroDuplicado() throws Exception {
        criarMesa(7);

        assertRegraNegocio(post("/mesa", json(7)),
                "Já existe uma mesa cadastrada com o número 7");

        assertThat(mesaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("alterar para o número de OUTRA mesa: 422; manter o próprio número é permitido")
    void alterarNumero() throws Exception {
        Mesa mesa1 = criarMesa(1);
        Mesa mesa2 = criarMesa(2);

        assertRegraNegocio(put("/mesa/" + mesa2.getId(), json(1)),
                "Já existe uma mesa cadastrada com o número 1");
        assertThat(mesaRepository.findById(mesa2.getId()).orElseThrow().getNumero()).isEqualTo(2);

        put("/mesa/" + mesa1.getId(), json(1)).andExpect(status().isOk());
        put("/mesa/" + mesa1.getId(), json(50))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(50));
    }

    // ------------------------------------------------------------------ IDs inexistentes / exclusão

    @Test
    @DisplayName("buscar e alterar mesa inexistente: 404")
    void mesaInexistente() throws Exception {
        get("/mesa/" + ID_INEXISTENTE).andExpect(status().isNotFound());
        put("/mesa/" + ID_INEXISTENTE, json(1)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("excluir mesa: 204, exclusão lógica; some da consulta e da listagem")
    void excluirMesa() throws Exception {
        Mesa mesa = criarMesa(4);

        get("/mesa").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        delete("/mesa/" + mesa.getId()).andExpect(status().isNoContent());

        assertThat(mesaRepository.findById(mesa.getId()).orElseThrow().getFlativo()).isEqualTo("N");
        get("/mesa/" + mesa.getId()).andExpect(status().isNotFound());
        get("/mesa").andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------ segurança

    @Test
    @DisplayName("requisição sem token é barrada e nada é gravado")
    void semToken() throws Exception {
        assertBarradoPelaSeguranca(postSemToken("/mesa", json(1)));

        assertThat(mesaRepository.count()).isZero();
    }
}
