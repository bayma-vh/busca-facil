# `ProdutoRepository`: acessando o banco sem escrever SQL

Segundo passo da camada de Produto (v0.1). A entidade (`docs/entidade-produto.md`) criou a **tabela**; o repository é quem **salva e busca** dados nela.

Arquivos:
- `backend/src/main/java/com/busca_facil/backend/produto/ProdutoRepository.java`
- `backend/src/test/java/com/busca_facil/backend/produto/ProdutoRepositoryTest.java`

---

## 1. Onde o repository se encaixa

```
Requisição HTTP ──► Controller ──► Service ──► Repository ──► Banco
                    (HTTP)         (regras)    (dados)        (MySQL)
```

Cada camada tem **uma** responsabilidade:

| Camada | Pergunta que responde | Status |
|---|---|---|
| Entidade | Como um produto é guardado? | ✅ feito |
| **Repository** | **Como salvar e buscar produtos?** | ✅ **este passo** |
| Service | Quais regras valem? (ex.: nome obrigatório) | próximo |
| Controller | Quais URLs a API expõe? | depois |

**Repository** é um padrão de projeto: um objeto que se comporta como uma "coleção" de entidades (adicionar, procurar, remover) e esconde *como* isso é feito no banco. O resto do código pede "me dê os produtos com 'note' no nome" sem saber se existe SQL, MySQL ou H2 por trás.

---

## 2. O código

```java
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByNomeContainingIgnoreCase(String nome);
}
```

São só duas linhas úteis. O resto vem de graça.

### `interface`, e não `class`

Em Java, uma **interface** declara métodos **sem implementá-los**. Normalmente alguém precisa escrever uma classe com o código de verdade. Aqui, quem escreve essa classe é o **Spring Data JPA**.

Ao iniciar, ele:
1. encontra interfaces que estendem `JpaRepository`, pela mesma varredura de pacotes que acha as entidades;
2. gera em tempo de execução uma classe que as implementa, chamada **proxy**;
3. registra essa classe como um **bean**, um objeto gerenciado pelo Spring, para ser injetado onde for pedido.

Por isso não existe um `ProdutoRepositoryImpl` no projeto, nem precisa existir.

### `extends JpaRepository<Produto, Long>`

Os dois **genéricos** (os tipos entre `< >`) dizem ao Spring Data:
- `Produto`: a entidade que este repository gerencia;
- `Long`: o tipo do `@Id` dela.

Com isso, herdamos métodos prontos, entre eles:

| Método | O que faz | SQL aproximado |
|---|---|---|
| `save(produto)` | Insere (se `id == null`) ou atualiza | `INSERT` ou `UPDATE` |
| `findById(id)` | Busca por id; devolve `Optional<Produto>` | `SELECT ... WHERE id = ?` |
| `findAll()` | Todos os produtos | `SELECT ... FROM produto` |
| `count()` | Quantos existem | `SELECT count(*)` |
| `existsById(id)` | Existe ou não | `SELECT ... WHERE id = ?` |
| `deleteById(id)` | Remove | `DELETE ... WHERE id = ?` |
| `findAll(Pageable)` | Busca paginada (página 1, 20 itens...) | `SELECT ... LIMIT ? OFFSET ?` |

**`Optional`**: o `findById` não devolve `Produto` direto, porque o produto pode não existir. O `Optional` é uma "caixa" que pode estar vazia, e obriga quem chama a tratar o caso de não encontrado, em vez de receber um `null` inesperado.

---

## 3. Derived queries: SQL a partir do nome do método

```java
List<Produto> findByNomeContainingIgnoreCase(String nome);
```

O Spring Data **lê o nome do método** e monta a consulta:

| Pedaço | Significado |
|---|---|
| `find` | É uma busca (`SELECT`) |
| `By` | A partir daqui vêm os filtros (`WHERE`) |
| `Nome` | O atributo `nome` da entidade |
| `Containing` | "Contém": vira `LIKE '%texto%'` |
| `IgnoreCase` | Ignora maiúsculas e minúsculas: aplica `upper()` nos dois lados |

O SQL gerado de verdade, copiado do log dos testes:

```sql
select p1_0.id, p1_0.categoria, p1_0.descricao, p1_0.nome
from produto p1_0
where upper(p1_0.nome) like upper(?) escape '\'
```

- `?` é um **parâmetro**. O Spring Data coloca ali `%NOTE%`. O valor é enviado **separado** do SQL, o que impede **SQL injection**: um usuário que digitasse `'; DROP TABLE produto; --` teria isso tratado como texto comum, e não como comando.
- `escape '\'`: se o usuário buscar por `%` ou `_`, que são curingas do `LIKE`, eles são tratados como caracteres normais.
- `p1_0` é só um apelido que o Hibernate dá à tabela.

Outros exemplos que funcionariam sem mudar mais nada:

```java
List<Produto> findByCategoria(String categoria);                     // WHERE categoria = ?
List<Produto> findByCategoriaOrderByNomeAsc(String categoria);       // ... ORDER BY nome
boolean existsByNomeIgnoreCase(String nome);                         // existe produto com esse nome?
long countByCategoria(String categoria);                             // SELECT count(*) ... WHERE categoria = ?
```

**Segurança extra:** se você escrever um nome inválido, como `findByPreco` (não existe atributo `preco` em Produto), a aplicação **falha ao iniciar**, com um erro dizendo que a propriedade não existe. O erro aparece logo, e não só quando alguém chama o método.

> Quando o nome do método ficar longo demais, a alternativa é escrever a consulta à mão com `@Query("select p from Produto p where ...")`, usando **JPQL**, uma linguagem parecida com SQL mas que fala de classes e atributos, e não de tabelas e colunas.

---

## 4. O teste: `@DataJpaTest`

```java
@DataJpaTest
class ProdutoRepositoryTest {

    @Autowired
    private ProdutoRepository repository;
    ...
}
```

### O que é `@DataJpaTest`

Um **slice test**, ou "teste de fatia". Em vez de subir a aplicação inteira, como o `@SpringBootTest`, ele sobe **só a fatia JPA**: entidades, repositories, Hibernate e um banco embutido (o H2 do Ajuste C). Não sobe controllers nem o servidor web. Resultado: o teste de repository levou cerca de 1,5 s, contra cerca de 7 s do teste completo.

No Spring Boot 4 essa anotação fica em `org.springframework.boot.data.jpa.test.autoconfigure`, vinda do starter `spring-boot-starter-data-jpa-test` que já estava no `pom.xml`. Em tutoriais antigos, do Spring Boot 2 e 3, ela aparece em outro pacote.

### Rollback automático

Cada método de teste roda dentro de uma **transação**, um bloco de operações no banco que pode ser confirmado (*commit*) ou desfeito (*rollback*). No `@DataJpaTest`, o padrão é **desfazer tudo** ao final de cada teste. Por isso o teste de busca encontra exatamente 2 notebooks: os produtos salvos no outro teste já foram apagados.

### `@Autowired`

Pede ao Spring: "me entregue o bean do tipo `ProdutoRepository`". Isso é a **injeção de dependência**: você não cria o objeto com `new`, o Spring fornece o proxy que ele gerou.

### Os dois testes

**`salvaProdutoEGeraId`**: salva um produto e confere que o `id` foi gerado e que o `findById` o encontra.

**`buscaPorParteDoNomeIgnorandoMaiusculas`**: salva "Notebook Dell", "Notebook Lenovo" e "Mouse sem fio", busca por `"NOTE"` (maiúsculo) e confere que vieram **só** os dois notebooks.

As verificações usam o **AssertJ** (`assertThat(...)`), uma biblioteca de *asserções* (afirmações que, se falsas, fazem o teste falhar) que já vem no starter de testes. Ela é escrita em estilo fluente: `assertThat(resultado).extracting(Produto::getNome).containsExactlyInAnyOrder(...)`.

- `extracting(Produto::getNome)`: transforma a lista de produtos numa lista só de nomes. `Produto::getNome` é uma **method reference**, uma forma curta de dizer "chame `getNome()` em cada um".
- `containsExactlyInAnyOrder`: a lista tem exatamente esses itens, em qualquer ordem.

---

## 5. O que o log revelou: o cache do Hibernate

Rodando `./mvnw test -Dspring.jpa.show-sql=true`, aparecem **4 `insert`** (1 do primeiro teste e 3 do segundo) e **só 1 `select`**, o da busca por nome.

Mas o primeiro teste chama `findById`. Por que não houve `SELECT`?

O Hibernate mantém um **contexto de persistência**, também chamado de **cache de primeiro nível**: um mapa em memória com as entidades que ele já conhece **dentro da transação atual**. Quando você faz `save`, o produto entra nesse mapa. O `findById` logo depois procura primeiro no mapa, encontra o produto e nem vai ao banco.

Duas consequências práticas:
- Isso economiza consultas repetidas.
- Às vezes o teste "passa" sem ter lido do banco de verdade. Quando for importante provar a leitura, existe o `TestEntityManager`, que permite chamar `flush()` (enviar as mudanças pendentes ao banco) e `clear()` (esvaziar o cache) antes da busca.

---

## 6. Como rodar

```bash
cd backend
./mvnw test                                    # todos os testes (3)
./mvnw test -Dtest=ProdutoRepositoryTest       # só os do repository
./mvnw test -Dtest=ProdutoRepositoryTest -Dspring.jpa.show-sql=true   # vendo o SQL
```

Saída esperada:
```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in ...ProdutoRepositoryTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 7. Próximo passo: `ProdutoService`

O repository sabe **guardar**, mas não sabe **regras**. Ele aceitaria salvar um produto com nome em branco, por exemplo, e só o `NOT NULL` do banco barraria um nome `null`, com um erro pouco amigável. A camada **Service** vai concentrar as regras de negócio e usar o repository por injeção de dependência, preparando o caminho para o Controller (`GET /produtos`, `POST /produtos`).
