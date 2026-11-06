package com.busca_facil.backend.produto;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// @RestController: cada método responde a uma requisição HTTP e o retorno vira JSON.
// @RequestMapping: prefixo comum a todas as rotas desta classe.
// O controller só traduz HTTP <-> Java; as regras ficam no ProdutoService.
@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    // GET /produtos            -> todos
    // GET /produtos?nome=note  -> filtra por parte do nome
    @GetMapping
    public List<ProdutoResponse> listar(@RequestParam(required = false) String nome) {
        return service.buscarPorNome(nome).stream()
                .map(ProdutoResponse::de)
                .toList();
    }

    // GET /produtos/5
    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return ProdutoResponse.de(service.buscarPorId(id));
    }

    // POST /produtos  (corpo JSON) -> 201 Created + cabeçalho Location: /produtos/{id}
    @PostMapping
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        Produto criado = service.criar(request.paraEntidade());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProdutoResponse.de(criado));
    }

    // PUT /produtos/5  (corpo JSON) -> substitui os dados do produto 5
    @PutMapping("/{id}")
    public ProdutoResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody ProdutoRequest request) {
        return ProdutoResponse.de(service.atualizar(id, request.paraEntidade()));
    }

    // DELETE /produtos/5 -> 204 No Content (sucesso, sem corpo)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
