package com.busca_facil.backend.produto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO de ENTRADA: só os campos que o cliente pode enviar (sem id).
// record = classe imutável e enxuta; o Java gera construtor, getters, equals e toString.
// As anotações de Bean Validation são checadas quando o controller usa @Valid.
public record ProdutoRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String nome,

        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
        String descricao,

        @Size(max = 80, message = "deve ter no máximo 80 caracteres")
        String categoria
) {

    // Converte o DTO na entidade que o service entende.
    public Produto paraEntidade() {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setCategoria(categoria);
        return produto;
    }
}
