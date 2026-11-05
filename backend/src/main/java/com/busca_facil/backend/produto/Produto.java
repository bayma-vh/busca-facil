package com.busca_facil.backend.produto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity                       // JPA: esta classe é mapeada para uma tabela
@Table(name = "produto")      // nome da tabela no MySQL
@Getter @Setter               // Lombok: gera getters e setters na compilação
@NoArgsConstructor            // JPA exige um construtor sem argumentos
public class Produto {

    @Id                                                   // chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // o MySQL gera o id (AUTO_INCREMENT)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(length = 80)
    private String categoria;
}
