package com.busca_facil.backend.produto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

// Teste de INTEGRAÇÃO: Service real + Repository real + H2.
// @DataJpaTest só sobe a camada JPA; @Import adiciona o ProdutoService ao contexto.
@DataJpaTest
@Import(ProdutoService.class)
class ProdutoServiceIntegracaoTest {

    @Autowired
    private ProdutoService service;

    @Autowired
    private TestEntityManager em;   // acesso direto ao EntityManager do Hibernate

    @Test
    void atualizarGravaNoBancoSemChamarSave() {
        Produto novo = new Produto();
        novo.setNome("Mouse");
        Long id = service.criar(novo).getId();

        service.atualizar(id, comNome("Mouse sem fio"));

        em.flush();   // força o Hibernate a enviar o UPDATE pendente ao banco
        em.clear();   // esvazia o cache: a próxima leitura vem do banco, não da memória

        Produto doBanco = em.find(Produto.class, id);
        assertThat(doBanco.getNome()).isEqualTo("Mouse sem fio");
    }

    private Produto comNome(String nome) {
        Produto p = new Produto();
        p.setNome(nome);
        return p;
    }
}
