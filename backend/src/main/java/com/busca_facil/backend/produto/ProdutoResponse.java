package com.busca_facil.backend.produto;

// DTO de SAÍDA: o formato do JSON devolvido pela API.
// Separado da entidade para que mudanças no banco não alterem o "contrato" da API.
public record ProdutoResponse(Long id, String nome, String descricao, String categoria) {

    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getCategoria());
    }
}
