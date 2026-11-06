# `ProdutoService`: a camada de regras de negócio

Terceiro passo da camada de Produto (v0.1). A entidade criou a **tabela** (`entidade-produto.md`), o repository sabe **salvar e buscar** (`produto-repository.md`). O service decide **o que é permitido**.

Arquivos:
- `backend/src/main/java/com/busca_facil/backend/produto/ProdutoService.java`
- `backend/src/main/java/com/busca_facil/backend/produto/ProdutoNaoEncontradoException.java`
- `backend/src/test/java/com/busca_facil/backend/produto/ProdutoServiceTest.java` (unitário)
- `backend/src/test/java/com/busca_facil/backend/produto/ProdutoServiceIntegracaoTest.java` (integração)

---

## 1. Por que uma camada só para regras?

```
Controller ──► Service ──► Repository ──► Banco
   (HTTP)      (regras)     (dados)
```

O repository **não julga**: aceitaria salvar um produto com nome `"   "` (só espaços). O controller lida com HTTP, ou seja, URLs, JSON e códigos de status. Se as regras ficassem no controller, elas se repetiriam em cada endpoint e não poderiam ser reaproveitadas, por exemplo por uma tarefa agendada que importasse produtos de uma loja.

O **service** é o lugar único onde mora a resposta para "isso pode?". Qualquer porta de entrada (HTTP, importação, testes) passa pelas mesmas regras.

---

## 2. `@Service` e injeção de dependência

```java
@Service
public class ProdutoService {

    private final ProdutoRepository repository;

    public ProdutoService(ProdutoRepository repository) {
        this.repository = repository;
    }
```

- **`@Service`**: marca a classe como um **bean**, um objeto que o Spring cria e gerencia. Por padrão existe **uma única instância** (*singleton*) compartilhada pela aplicação. Tecnicamente é igual a `@Component`; o nome `@Service` só deixa claro o papel da classe.
- **Injeção de dependência (DI)**: o service *precisa* de um repository, mas não o cria com `new`. Ele o **declara no construtor**, e o Spring entrega o proxy que gerou (`produto-repository.md`, seção 2).
- **Sem `@Autowired`**: quando a classe tem um único construtor, o Spring o usa automaticamente.

Por que **construtor + `final`**, e não `@Autowired` direto no campo?

| | Construtor + `final` | `@Autowired` no campo |
|---|---|---|
| Dependência obrigatória | Sim: sem repository, o objeto nem é criado | Pode ficar `null` |
| Imutável | Sim (`final`) | Não |
| Testar sem Spring | Fácil: `new ProdutoService(mock)` | Precisa de reflexão ou do Spring |
| Dependências visíveis | Na assinatura do construtor | Escondidas nos campos |

> Com o Lombok, `@RequiredArgsConstructor` gera esse construtor sozinho, para todos os campos `final`. Deixamos explícito aqui para ficar visível o que acontece.

---

## 3. As regras de negócio

| Regra | Onde | Por quê |
|---|---|---|
| Nome obrigatório (nem `null`, nem só espaços) | `normalizarEValidar` | Produto sem nome não faz sentido na busca |
| `trim()` em nome, descrição e categoria | `limpar` | `"  Notebook  "` e `"Notebook"` são o mesmo produto |
| Texto vazio vira `null` | `limpar` | Não guardar `"   "` no banco; "sem categoria" é `null` |
| `criar` ignora o `id` recebido | `criar` | Se o cliente mandasse `id: 5`, o `save` **atualizaria** o produto 5 em vez de criar um novo |
| Busca com termo vazio devolve todos | `buscarPorNome` | `LIKE '%%'` funcionaria, mas é desperdício; e `null` quebraria a consulta |
| Id inexistente lança `ProdutoNaoEncontradoException` | `buscarPorId`, `remover` | Erro claro em vez de `null` ou de uma remoção silenciosa |

### A regra do `id` no `criar`

Lembre do `produto-repository.md`: o `save` decide entre `INSERT` e `UPDATE` olhando o `id`. Se `id == null`, insere; se não for, atualiza. Por isso o `criar` faz `produto.setId(null)` **antes** do `save`. Sem essa linha, um cliente mal-intencionado poderia sobrescrever qualquer produto pela rota de criação.

---

## 4. Exceções

```java
public class ProdutoNaoEncontradoException extends RuntimeException {
    public ProdutoNaoEncontradoException(Long id) {
        super("Produto não encontrado: id " + id);
    }
}
```

Em Java há dois tipos de exceção:
- **Verificadas** (*checked*, filhas de `Exception`): o compilador **obriga** quem chama a tratar com `try/catch` ou a declarar `throws`.
- **Não verificadas** (*unchecked*, filhas de `RuntimeException`): não obrigam nada.

Usamos `RuntimeException` porque é a convenção no Spring: a exceção "sobe" livremente até um ponto central que a trata. No passo do Controller, esse ponto será um `@RestControllerAdvice`, que vai transformar:

| Exceção | Resposta HTTP |
|---|---|
| `ProdutoNaoEncontradoException` | `404 Not Found` |
| `IllegalArgumentException` (nome em branco) | `400 Bad Request` |

E o `orElseThrow`?

```java
return repository.findById(id)
        .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
```

O `findById` devolve um `Optional<Produto>`, uma "caixa" que pode estar vazia. O `orElseThrow` diz: "se tiver produto, devolva; se estiver vazia, lance esta exceção". O `() -> ...` é uma **lambda**, uma função curta passada como argumento, que só é executada se a caixa estiver vazia.

---

## 5. Transações: `@Transactional`

Uma **transação** é um conjunto de operações no banco que vale **tudo ou nada**. Se algo falhar no meio, tudo é desfeito (*rollback*). Se tudo der certo, é confirmado (*commit*).

```java
@Transactional
public Produto criar(Produto produto) { ... }

@Transactional(readOnly = true)
public List<Produto> listarTodos() { ... }
```

Como funciona: o Spring envolve o `ProdutoService` num **proxy**, uma classe "embrulho". Quando alguém chama `criar`, o proxy:
1. abre a transação;
2. chama o seu método;
3. se o método terminou normalmente, faz **commit**; se lançou uma `RuntimeException`, faz **rollback**.

`readOnly = true` avisa que o método só lê. O Hibernate pula a checagem de mudanças, e alguns bancos otimizam a leitura.

> Cuidado: como funciona por proxy, o `@Transactional` só vale para chamadas que vêm **de fora** da classe. Se um método do próprio service chama outro método do mesmo service, a chamada não passa pelo proxy. No `atualizar`, a chamada a `buscarPorId` já está dentro da transação do `atualizar`, então não há problema.

---

## 6. Dirty checking: o `atualizar` sem `save`

```java
@Transactional
public Produto atualizar(Long id, Produto dados) {
    Produto existente = buscarPorId(id);
    existente.setNome(dados.getNome());
    ...
    return existente;   // nenhum save()!
}
```

Como o `UPDATE` acontece?

1. `buscarPorId` carrega o produto. Ele passa a ser **gerenciado** (*managed*): o Hibernate o guarda no contexto de persistência, junto com uma "foto" de como ele estava.
2. Os `set...` alteram o objeto em memória.
3. Ao final da transação, antes do commit, o Hibernate compara cada objeto gerenciado com a foto. Se algo mudou, ele está **"sujo"** (*dirty*), e o Hibernate gera o `UPDATE`.

Isso é o **dirty checking**. A prova está no log do teste de integração:

```
Hibernate: insert into produto (categoria,descricao,nome,id) values (?,?,?,default)
Hibernate: update produto set categoria=?,descricao=?,nome=? where id=?     ← sem save()
Hibernate: select p1_0.id,... from produto p1_0 where p1_0.id=?             ← releitura do banco
```

> Chamar `save` no `atualizar` não estaria errado, só seria redundante. Muita gente chama por clareza. O importante é entender que **não é o `save` que gera o UPDATE** de um objeto gerenciado.

### Por que não usar `dados` direto?

O `atualizar` copia campo a campo de `dados` para `existente`, em vez de salvar `dados`. Assim, decidimos **quais campos podem mudar**: o `id`, por exemplo, nunca é copiado. No passo do Controller, `dados` virá de um **DTO**, uma classe só com os campos que o cliente pode enviar, em vez da entidade.

---

## 7. Dois tipos de teste

### Teste unitário com Mockito: `ProdutoServiceTest`

```java
@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {
    @Mock        private ProdutoRepository repository;  // falso
    @InjectMocks private ProdutoService service;        // real, recebe o falso
```

- **Mockito**: biblioteca que cria **mocks**, objetos falsos que imitam uma interface. Um mock não faz nada até você programá-lo:
  ```java
  when(repository.findById(42L)).thenReturn(Optional.empty());   // "quando pedirem o 42, diga que não existe"
  ```
- `@ExtendWith(MockitoExtension.class)`: liga o Mockito ao JUnit 5. **Não sobe o Spring.**
- `@InjectMocks`: cria o `ProdutoService` real passando o mock no construtor. A injeção por construtor (seção 2) é o que torna isso simples.
- `verify(...)`: confere **se** e **como** o mock foi chamado:
  ```java
  verify(repository, never()).save(any());   // garante que, com nome em branco, nada foi salvo
  ```
- `assertThatThrownBy(...)`: confere que o código **lançou** a exceção esperada, com a mensagem esperada.
- `thenAnswer(inv -> inv.getArgument(0))`: faz o `save` falso devolver o próprio objeto recebido, como o real faria.

São 6 testes, que rodam em cerca de **0,4 s**, sem banco.

### Teste de integração: `ProdutoServiceIntegracaoTest`

O teste unitário prova que o `save` **não é chamado** no `atualizar`. Mas, com um repository falso, ele **não consegue provar** que o banco é atualizado, porque não há Hibernate nem banco. Para isso existe o teste de integração:

```java
@DataJpaTest
@Import(ProdutoService.class)
class ProdutoServiceIntegracaoTest {
    @Autowired private ProdutoService service;
    @Autowired private TestEntityManager em;
```

- `@DataJpaTest` sobe a fatia JPA com o H2. Ela não inclui `@Service`s, então o `@Import(ProdutoService.class)` adiciona o service ao contexto.
- **`TestEntityManager`**: acesso direto ao Hibernate dentro do teste. No Spring Boot 4 ele fica em `org.springframework.boot.jpa.test.autoconfigure`.
  - `em.flush()`: **envia agora** ao banco as mudanças pendentes. Foi aqui que o `UPDATE` apareceu, já que o teste faz rollback no final e nunca chegaria ao commit.
  - `em.clear()`: **esvazia o cache**. Sem isso, o `em.find` devolveria o objeto da memória, como vimos no `produto-repository.md`, seção 5, e o teste passaria sem provar nada.

| | Unitário (Mockito) | Integração (`@DataJpaTest`) |
|---|---|---|
| Sobe o Spring? | Não | Só a fatia JPA |
| Banco | Nenhum (mock) | H2 |
| Velocidade | Milissegundos | Cerca de 0,4 s, mais a inicialização do contexto |
| Prova | As **regras** do service | Que o service **funciona com o banco de verdade** |

A combinação é o ideal: muitos testes unitários, rápidos, para as regras, e poucos de integração para o que só o banco mostra.

---

## 8. Como rodar

```bash
cd backend
./mvnw test                                       # todos (10 testes)
./mvnw test -Dtest=ProdutoServiceTest             # só os unitários do service
./mvnw test -Dtest=ProdutoServiceIntegracaoTest -Dspring.jpa.show-sql=true   # ver o UPDATE
./mvnw test -Dtest='Produto*'                     # tudo que começa com Produto
```

Saída esperada:
```
Tests run: 6, ... -- in ...ProdutoServiceTest
Tests run: 1, ... -- in ...ProdutoServiceIntegracaoTest
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 9. Próximo passo: `ProdutoController`

Falta a porta de entrada HTTP. O controller vai:
- expor `GET /produtos`, `GET /produtos/{id}`, `GET /produtos?nome=...`, `POST /produtos`, `PUT /produtos/{id}` e `DELETE /produtos/{id}`;
- receber e devolver **JSON** usando **DTOs**, em vez de expor a entidade;
- usar **Bean Validation** (`@NotBlank`, `@Size`) para recusar dados inválidos logo na entrada;
- traduzir as exceções do service em códigos HTTP (404, 400) com um `@RestControllerAdvice`.
