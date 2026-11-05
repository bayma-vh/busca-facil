package com.busca_facil.backend.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Teste UNITÁRIO: sem Spring e sem banco. O repository é um "mock" (objeto falso)
// cujas respostas o próprio teste define. Testamos só as regras do Service.
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository repository;   // falso, criado pelo Mockito

    @InjectMocks
    private ProdutoService service;         // real, recebe o mock no construtor

    @Test
    void criarLimpaEspacosIgnoraIdESalva() {
        Produto entrada = produto(99L, "  Notebook Dell  ", "   ");
        when(repository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto salvo = service.criar(entrada);

        assertThat(salvo.getId()).isNull();                 // id 99 foi ignorado
        assertThat(salvo.getNome()).isEqualTo("Notebook Dell");
        assertThat(salvo.getCategoria()).isNull();          // "   " virou null
        verify(repository).save(entrada);
    }

    @Test
    void criarRecusaNomeEmBranco() {
        Produto entrada = produto(null, "   ", "Informática");

        assertThatThrownBy(() -> service.criar(entrada))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome");

        verify(repository, never()).save(any());            // nada foi salvo
    }

    @Test
    void buscarPorIdInexistenteLancaExcecao() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(42L))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessageContaining("42");
    }

    @Test
    void buscarPorNomeVazioDevolveTodos() {
        List<Produto> todos = List.of(produto(1L, "Mouse", null));
        when(repository.findAll()).thenReturn(todos);

        assertThat(service.buscarPorNome("  ")).isEqualTo(todos);
        verify(repository, never()).findByNomeContainingIgnoreCase(any());
    }

    @Test
    void atualizarAlteraCamposDoProdutoExistente() {
        Produto existente = produto(1L, "Mouse", "Periféricos");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        Produto resultado = service.atualizar(1L, produto(null, " Mouse sem fio ", "Periféricos"));

        assertThat(resultado).isSameAs(existente);          // o mesmo objeto, modificado
        assertThat(resultado.getNome()).isEqualTo("Mouse sem fio");
        verify(repository, never()).save(any());            // dirty checking: sem save()
    }

    @Test
    void removerInexistenteLancaExcecaoENaoApaga() {
        when(repository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.remover(7L))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
        verify(repository, never()).deleteById(any());
    }

    private Produto produto(Long id, String nome, String categoria) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setCategoria(categoria);
        return p;
    }
}
