package com.busca_facil.backend.produto;

// Exceção de negócio: "não existe produto com esse id".
// RuntimeException = exceção "não verificada": quem chama não é obrigado a usar try/catch.
// No passo do Controller ela será traduzida para HTTP 404 (Not Found).
public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto não encontrado: id " + id);
    }
}
