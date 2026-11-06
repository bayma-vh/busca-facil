package com.busca_facil.backend.produto;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// @Service: o Spring cria um único objeto (bean) desta classe e o injeta onde for pedido.
// Aqui ficam as REGRAS DE NEGÓCIO; o acesso ao banco é delegado ao repository.
@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    // Injeção de dependência pelo construtor: o Spring passa o ProdutoRepository.
    // Com um único construtor, nem é preciso @Autowired.
    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Produto> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    // Termo vazio ou só espaços = sem filtro: devolve todos.
    @Transactional(readOnly = true)
    public List<Produto> buscarPorNome(String termo) {
        if (termo == null || termo.isBlank()) {
            return repository.findAll();
        }
        return repository.findByNomeContainingIgnoreCase(termo.trim());
    }

    @Transactional
    public Produto criar(Produto produto) {
        produto.setId(null); // ignora id enviado: criar nunca sobrescreve um produto existente
        normalizarEValidar(produto);
        return repository.save(produto);
    }

    // Não chama save(): dentro da transação o Hibernate detecta que o objeto mudou
    // ("dirty checking") e gera o UPDATE sozinho ao final do método.
    @Transactional
    public Produto atualizar(Long id, Produto dados) {
        Produto existente = buscarPorId(id);
        existente.setNome(dados.getNome());
        existente.setDescricao(dados.getDescricao());
        existente.setCategoria(dados.getCategoria());
        normalizarEValidar(existente);
        return existente;
    }

    @Transactional
    public void remover(Long id) {
        if (!repository.existsById(id)) {
            throw new ProdutoNaoEncontradoException(id);
        }
        repository.deleteById(id);
    }

    // ---------- Regras de negócio ----------

    private void normalizarEValidar(Produto produto) {
        produto.setNome(limpar(produto.getNome()));
        produto.setDescricao(limpar(produto.getDescricao()));
        produto.setCategoria(limpar(produto.getCategoria()));

        if (produto.getNome() == null) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
    }

    // Remove espaços das pontas; texto vazio vira null (não guardamos "   " no banco).
    private String limpar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}
