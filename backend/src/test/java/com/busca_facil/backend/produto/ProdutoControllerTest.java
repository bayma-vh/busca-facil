package com.busca_facil.backend.produto;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// @WebMvcTest: sobe só a camada web (controller, JSON, validação, @RestControllerAdvice).
// Sem banco e sem servidor: o MockMvc simula as requisições HTTP em memória.
// O ProdutoService é substituído por um mock (@MockitoBean).
// Os @RestControllerAdvice (TratadorDeErros) são carregados automaticamente.
@WebMvcTest(ProdutoController.class)
class ProdutoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProdutoService service;

    @Test
    void listarDevolve200ComLista() throws Exception {
        when(service.buscarPorNome("note")).thenReturn(List.of(produto(1L, "Notebook Dell")));

        mvc.perform(get("/produtos").param("nome", "note"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Notebook Dell"));
    }

    @Test
    void buscarInexistenteDevolve404ComProblemDetail() throws Exception {
        when(service.buscarPorId(42L)).thenThrow(new ProdutoNaoEncontradoException(42L));

        mvc.perform(get("/produtos/42"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Produto não encontrado: id 42"));
    }

    @Test
    void criarDevolve201ComLocation() throws Exception {
        when(service.criar(any(Produto.class))).thenReturn(produto(10L, "Mouse sem fio"));

        mvc.perform(post("/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Mouse sem fio", "categoria": "Periféricos" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/produtos/10")))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Mouse sem fio"));
    }

    @Test
    void criarSemNomeDevolve400ENaoChamaService() throws Exception {
        mvc.perform(post("/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "", "categoria": "Periféricos" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.nome").value("é obrigatório"));

        verify(service, never()).criar(any());
    }

    @Test
    void atualizarDevolve200() throws Exception {
        when(service.atualizar(eq(1L), any(Produto.class))).thenReturn(produto(1L, "Mouse gamer"));

        mvc.perform(put("/produtos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Mouse gamer" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Mouse gamer"));
    }

    @Test
    void removerDevolve204() throws Exception {
        mvc.perform(delete("/produtos/1"))
                .andExpect(status().isNoContent());

        verify(service).remover(1L);
    }

    @Test
    void removerInexistenteDevolve404() throws Exception {
        doThrow(new ProdutoNaoEncontradoException(7L)).when(service).remover(7L);

        mvc.perform(delete("/produtos/7"))
                .andExpect(status().isNotFound());
    }

    private Produto produto(Long id, String nome) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        return p;
    }
}
