package com.busca_facil.backend.erro;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.busca_facil.backend.produto.ProdutoNaoEncontradoException;

// @RestControllerAdvice: "vigia" todos os controllers. Quando um deles lança uma exceção,
// o método @ExceptionHandler correspondente monta a resposta HTTP.
// ProblemDetail = formato padrão de erro em JSON (RFC 9457), Content-Type application/problem+json.
@RestControllerAdvice
public class TratadorDeErros {

    // Id inexistente -> 404 Not Found
    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(ProdutoNaoEncontradoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    // Regra de negócio violada no service (ex.: nome em branco) -> 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail regraViolada(IllegalArgumentException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Requisição inválida");
        return problema;
    }

    // @Valid falhou no DTO -> 400 Bad Request, listando o erro de cada campo
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.");
        problema.setTitle("Dados inválidos");
        problema.setProperty("erros", erros);   // campo extra no JSON: { "nome": "é obrigatório" }
        return problema;
    }
}
