package com.busca_facil.backend.produto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

// Interface sem implementação: o Spring Data cria a classe em tempo de execução.
// <Produto, Long> = entidade gerenciada e tipo do seu @Id.
// Já herda save, findById, findAll, deleteById, count, existsById...
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // Derived query: o SQL é gerado a partir do nome do método.
    // findBy + Nome + Containing (LIKE %x%) + IgnoreCase (ignora maiúsculas)
    // -> select ... from produto where upper(nome) like upper(?)   (com ? = "%texto%")
    List<Produto> findByNomeContainingIgnoreCase(String nome);
}
