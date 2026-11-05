package com.busca_facil.backend.produto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

// @DataJpaTest sobe só a camada JPA (entidades + repositories + H2).
// Cada teste roda numa transação que é desfeita (rollback) no final.
@DataJpaTest
class ProdutoRepositoryTest {

    @Autowired
    private ProdutoRepository repository;

    @Test
    void salvaProdutoEGeraId() {
        Produto produto = novoProduto("Notebook Dell", "Informática");

        Produto salvo = repository.save(produto);

        assertThat(salvo.getId()).isNotNull();
        assertThat(repository.findById(salvo.getId()))
                .get()
                .extracting(Produto::getNome)
                .isEqualTo("Notebook Dell");
    }

    @Test
    void buscaPorParteDoNomeIgnorandoMaiusculas() {
        repository.save(novoProduto("Notebook Dell", "Informática"));
        repository.save(novoProduto("Notebook Lenovo", "Informática"));
        repository.save(novoProduto("Mouse sem fio", "Periféricos"));

        List<Produto> resultado = repository.findByNomeContainingIgnoreCase("NOTE");

        assertThat(resultado)
                .extracting(Produto::getNome)
                .containsExactlyInAnyOrder("Notebook Dell", "Notebook Lenovo");
    }

    private Produto novoProduto(String nome, String categoria) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setCategoria(categoria);
        return p;
    }
}
