package com.easyeats.api.support;

import com.easyeats.api.entity.Categoria;
import com.easyeats.api.entity.Estoque;
import com.easyeats.api.entity.Ingrediente;
import com.easyeats.api.entity.Mesa;
import com.easyeats.api.entity.Preco;
import com.easyeats.api.entity.Produto;
import com.easyeats.api.entity.ProdutoIngrediente;
import com.easyeats.api.entity.ProdutoIngredienteId;
import com.easyeats.api.entity.Usuario;
import com.easyeats.api.repository.CategoriaRepository;
import com.easyeats.api.repository.EstoqueRepository;
import com.easyeats.api.repository.IngredienteRepository;
import com.easyeats.api.repository.MesaRepository;
import com.easyeats.api.repository.PedidoRepository;
import com.easyeats.api.repository.PrecoRepository;
import com.easyeats.api.repository.ProdutoIngredienteRepository;
import com.easyeats.api.repository.ProdutoRepository;
import com.easyeats.api.repository.UsuarioRepository;
import com.easyeats.api.security.JwtUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração.
 *
 * - Sobe o contexto completo da aplicação com o profile "test" (H2 em memória).
 * - Monta o MockMvc COM a cadeia de filtros do Spring Security, então o JWT é exercitado de verdade.
 * - Autentica as requisições com um token gerado pelo próprio JwtUtil (sem depender de spring-security-test).
 * - Limpa as tabelas de negócio antes de cada teste. Os testes NÃO são @Transactional de propósito:
 *   assim os commits/rollbacks das services (ex.: cadastro em lote "tudo ou nada") são exercitados de verdade.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final long ID_INEXISTENTE = 999_999L;

    private static final String USUARIO_TESTE = "usuario.teste";

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UsuarioRepository usuarioRepository;

    @Autowired protected CategoriaRepository categoriaRepository;
    @Autowired protected IngredienteRepository ingredienteRepository;
    @Autowired protected ProdutoRepository produtoRepository;
    @Autowired protected PrecoRepository precoRepository;
    @Autowired protected ProdutoIngredienteRepository produtoIngredienteRepository;
    @Autowired protected MesaRepository mesaRepository;
    @Autowired protected PedidoRepository pedidoRepository;
    @Autowired protected EstoqueRepository estoqueRepository;

    protected MockMvc mockMvc;
    private String token;

    @BeforeEach
    protected void prepararAmbiente() {
        limparBanco();

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        token = gerarToken();
    }

    // ------------------------------------------------------------------ infraestrutura

    private void limparBanco() {
        // ordem respeita as chaves estrangeiras (filhos antes dos pais)
        pedidoRepository.deleteAll();               // itens do pedido saem por cascade
        produtoIngredienteRepository.deleteAll();
        precoRepository.deleteAll();
        produtoRepository.deleteAll();
        estoqueRepository.deleteAllInBatch();
        ingredienteRepository.deleteAll();
        categoriaRepository.deleteAll();
        mesaRepository.deleteAll();
    }

    private String gerarToken() {
        usuarioRepository.findByUsername(USUARIO_TESTE).orElseGet(() -> {
            Usuario usuario = new Usuario();
            usuario.setUsername(USUARIO_TESTE);
            usuario.setPassword("nao-usada-nos-testes");
            return usuarioRepository.save(usuario);
        });

        return jwtUtil.generateToken(userDetailsService.loadUserByUsername(USUARIO_TESTE));
    }

    // ------------------------------------------------------------------ requisições autenticadas

    protected ResultActions post(String url, String json) throws Exception {
        return mockMvc.perform(comToken(MockMvcRequestBuilders.post(url)).content(json));
    }

    protected ResultActions put(String url, String json) throws Exception {
        return mockMvc.perform(comToken(MockMvcRequestBuilders.put(url)).content(json));
    }

    protected ResultActions get(String url) throws Exception {
        return mockMvc.perform(comToken(MockMvcRequestBuilders.get(url)));
    }

    protected ResultActions delete(String url) throws Exception {
        return mockMvc.perform(comToken(MockMvcRequestBuilders.delete(url)));
    }

    protected ResultActions patch(String url) throws Exception {
        return mockMvc.perform(comToken(MockMvcRequestBuilders.patch(url)));
    }

    /** Mesma requisição, mas sem o header Authorization. */
    protected ResultActions postSemToken(String url, String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder builder) {
        return builder
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);
    }

    // ------------------------------------------------------------------ asserts reutilizáveis

    /** 400 com o formato { "erro": "Dados inválidos", "campos": ["campo: mensagem", ...] }. */
    protected void assertErroValidacao(ResultActions resultado, String... camposEsperados) throws Exception {
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Dados inválidos"));

        List<String> campos = JsonPath.read(corpo(resultado), "$.campos");
        assertThat(campos).contains(camposEsperados);
    }

    /** 422 com { "erro": mensagem } (RegraNegocioException). */
    protected void assertRegraNegocio(ResultActions resultado, String mensagem) throws Exception {
        resultado.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(mensagem));
    }

    /** 404 com { "erro": mensagem } (RecursoNaoEncontradoException). */
    protected void assertNaoEncontrado(ResultActions resultado, String mensagem) throws Exception {
        resultado.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value(mensagem));
    }

    /** 401 ou 403: o importante é que a requisição anônima seja barrada. */
    protected void assertBarradoPelaSeguranca(ResultActions resultado) throws Exception {
        int status = resultado.andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }

    protected String corpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected Long idDaResposta(ResultActions resultado) throws Exception {
        Number id = JsonPath.read(corpo(resultado), "$.id");
        return id.longValue();
    }

    // ------------------------------------------------------------------ fixtures (gravadas direto no banco)

    protected Categoria criarCategoria(String nome) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setFlativo("S");
        return categoriaRepository.save(categoria);
    }

    protected Ingrediente criarIngrediente(String nome) {
        return salvarIngrediente(nome, "S");
    }

    protected Ingrediente criarIngredienteInativo(String nome) {
        return salvarIngrediente(nome, "N");
    }

    private Ingrediente salvarIngrediente(String nome, String flativo) {
        Ingrediente ingrediente = new Ingrediente();
        ingrediente.setNome(nome);
        ingrediente.setUnidadeMedida("kg");
        ingrediente.setCusto(new BigDecimal("5.00"));
        ingrediente.setFlativo(flativo);
        return ingredienteRepository.save(ingrediente);
    }

    /** Produto ativo. Se {@code preco} for null, o produto fica sem preço ativo. */
    protected Produto criarProduto(String nome, Categoria categoria, BigDecimal preco) {
        return salvarProduto(nome, categoria, preco, "S");
    }

    protected Produto criarProdutoInativo(String nome, Categoria categoria, BigDecimal preco) {
        return salvarProduto(nome, categoria, preco, "N");
    }

    private Produto salvarProduto(String nome, Categoria categoria, BigDecimal valor, String flativo) {
        Produto produto = produtoRepository.save(Produto.builder()
                .nome(nome)
                .descricao("Descrição de " + nome)
                .flativo(flativo)
                .categoria(categoria)
                .build());

        if (valor != null) {
            Preco preco = new Preco();
            preco.setValor(valor);
            preco.setProduto(produto);
            preco.setFlativo("S");
            precoRepository.save(preco);
        }

        return produto;
    }

    protected Mesa criarMesa(int numero) {
        return salvarMesa(numero, "S");
    }

    protected Mesa criarMesaInativa(int numero) {
        return salvarMesa(numero, "N");
    }

    private Mesa salvarMesa(int numero, String flativo) {
        Mesa mesa = new Mesa();
        mesa.setNumero(numero);
        mesa.setFlativo(flativo);
        return mesaRepository.save(mesa);
    }

    protected ProdutoIngrediente vincular(Produto produto, Ingrediente ingrediente, double quantidade) {
        ProdutoIngrediente vinculo = new ProdutoIngrediente();
        vinculo.setId(new ProdutoIngredienteId(produto.getId(), ingrediente.getId()));
        vinculo.setProduto(produto);
        vinculo.setIngrediente(ingrediente);
        vinculo.setQuantidade(quantidade);
        return produtoIngredienteRepository.save(vinculo);
    }

    protected Estoque criarEstoque(Ingrediente ingrediente, int qtdAtual) {
        return criarEstoque(ingrediente, BigDecimal.valueOf(qtdAtual));
    }

    protected Estoque criarEstoque(Ingrediente ingrediente, BigDecimal qtdAtual) {
        Estoque estoque = new Estoque();
        estoque.setIngrediente(ingrediente);
        estoque.setQtdAtual(qtdAtual);
        estoque.setQtdMinima(BigDecimal.ZERO);
        return estoqueRepository.save(estoque);
    }
}
