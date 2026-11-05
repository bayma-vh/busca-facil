# `ProdutoController`: a API REST de produtos

Quarto e último passo da camada de Produto (v0.1). Com ele, o Busca Fácil passa a ter uma **API HTTP** funcionando: qualquer programa (navegador, `curl`, Postman, e futuramente o front-end Next.js) pode cadastrar, consultar, alterar e remover produtos.

Camadas já prontas, cada uma com seu documento:

| Camada | Responsabilidade | Documento |
|---|---|---|
| Entidade `Produto` | Como o produto é guardado no banco | `entidade-produto.md` |
| `ProdutoRepository` | Salvar e buscar no banco | `produto-repository.md` |
| `ProdutoService` | Regras de negócio | `produto-service.md` |
| **`ProdutoController`** | **Receber requisições HTTP e devolver JSON** | **este documento** |

Este documento tem duas partes:
- **Parte 1, como funciona**: os arquivos, as anotações e o caminho de uma requisição.
- **Parte 2, como usar**: referência dos endpoints e exemplos prontos com `curl` e PowerShell.

---

# Parte 1: como funciona

## 1. Arquivos criados ou alterados

| Arquivo | O que é |
|---|---|
| `produto/ProdutoController.java` | Os endpoints (rotas) da API |
| `produto/ProdutoRequest.java` | DTO de **entrada**: o JSON que o cliente envia |
| `produto/ProdutoResponse.java` | DTO de **saída**: o JSON que a API devolve |
| `erro/TratadorDeErros.java` | Transforma exceções em respostas de erro padronizadas |
| `application.properties` | Duas configurações novas: `open-in-view` e `problemdetails` (seção 7) |
| `test/.../ProdutoControllerTest.java` | 7 testes da camada web |

Todos os caminhos começam em `backend/src/main/java/com/busca_facil/backend/`, ou em `backend/src/test/java/...` no caso do teste.

## 2. Conceitos básicos: HTTP e REST

**HTTP** é o protocolo que navegadores e aplicações usam para conversar com servidores. Toda conversa é um par **requisição → resposta**:

```
Requisição                                  Resposta
─────────────────────────────────           ─────────────────────────────────
POST /produtos              ← método+URL    HTTP/1.1 201                ← status
Content-Type: application/json ← cabeçalho  Location: /produtos/1       ← cabeçalho
                                            Content-Type: application/json
{"nome":"Notebook Dell"}    ← corpo         {"id":1,"nome":"Notebook Dell",...} ← corpo
```

- **Método** (ou verbo): diz a **intenção**. `GET` consulta, `POST` cria, `PUT` substitui, `DELETE` remove.
- **URL**: diz **sobre o que** é a requisição. `/produtos` é a coleção; `/produtos/1` é o produto 1.
- **Cabeçalhos**: metadados. `Content-Type` informa o formato do corpo.
- **Corpo**: os dados, aqui em **JSON** (*JavaScript Object Notation*), um formato de texto com chaves `{}`, listas `[]` e pares `"campo": valor`.
- **Status**: um número de 3 dígitos com o resultado. `2xx` é sucesso, `4xx` é erro do cliente e `5xx` é erro do servidor.

**REST** é um estilo de organizar APIs: as URLs representam **recursos** (substantivos, como `/produtos`), e o método HTTP diz **o que fazer** com eles. Por isso não existem URLs como `/criarProduto` ou `/deletarProduto?id=1`.

## 3. O caminho de uma requisição

O que acontece quando chega um `POST /produtos`:

```
1. Tomcat (servidor web embutido) recebe os bytes na porta 8080
2. DispatcherServlet (o "porteiro" do Spring MVC) procura qual método atende POST /produtos
        → encontra ProdutoController.criar(...)
3. Jackson converte o JSON do corpo em um objeto ProdutoRequest
4. Bean Validation checa as anotações (@NotBlank, @Size) por causa do @Valid
        ✗ inválido → MethodArgumentNotValidException → TratadorDeErros → 400
5. O controller chama service.criar(request.paraEntidade())
        ✗ regra violada → IllegalArgumentException → TratadorDeErros → 400
6. Service → Repository → Hibernate → INSERT no MySQL
7. O controller monta a resposta: status 201, cabeçalho Location e o ProdutoResponse
8. Jackson converte o ProdutoResponse em JSON e o Tomcat envia a resposta
```

- **Tomcat**: o servidor web que vem **dentro** da aplicação Spring Boot. Por isso basta rodar o `.jar` (`java -jar app.jar`) para ter um servidor no ar, sem instalar nada à parte.
- **Spring MVC**: a parte do Spring que faz o roteamento URL → método. Vem do starter `spring-boot-starter-webmvc`.
- **Jackson**: a biblioteca que converte Java ↔ JSON. A conversão de JSON para objeto se chama *desserialização*, e a de objeto para JSON, *serialização*.

## 4. O controller, anotação por anotação

```java
@RestController
@RequestMapping("/produtos")
public class ProdutoController {
```

- **`@RestController`**: registra a classe como controller **e** indica que o retorno de cada método vai direto para o corpo da resposta, convertido em JSON. É a soma de `@Controller` + `@ResponseBody`.
- **`@RequestMapping("/produtos")`**: prefixo comum. Todas as rotas da classe começam com `/produtos`.
- O service chega pelo **construtor**, a mesma injeção de dependência explicada em `produto-service.md`, seção 2.

### Mapeando métodos HTTP

| Anotação | Atende | Exemplo nesta classe |
|---|---|---|
| `@GetMapping` | `GET` | `listar`, `buscar` |
| `@PostMapping` | `POST` | `criar` |
| `@PutMapping` | `PUT` | `atualizar` |
| `@DeleteMapping` | `DELETE` | `remover` |

Sem argumento (`@GetMapping`), a rota é o próprio prefixo, `/produtos`. Com argumento (`@GetMapping("/{id}")`), a rota é `/produtos/{id}`.

### De onde vêm os parâmetros

```java
public ProdutoResponse buscar(@PathVariable Long id)
```
**`@PathVariable`**: pega um pedaço **da URL**. Em `/produtos/5`, o `{id}` vale `5`. O Spring já converte o texto `"5"` para `Long`. Se a URL for `/produtos/abc`, a conversão falha e a resposta é `400`.

```java
public List<ProdutoResponse> listar(@RequestParam(required = false) String nome)
```
**`@RequestParam`**: pega um **parâmetro de consulta** (*query string*), o que vem depois do `?`. Em `/produtos?nome=note`, `nome` vale `"note"`. Com `required = false`, ele é opcional: em `/produtos` puro, `nome` vale `null`, e o service devolve todos.

```java
public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request)
```
- **`@RequestBody`**: pega o **corpo** da requisição e o converte, via Jackson, num `ProdutoRequest`.
- **`@Valid`**: manda checar as anotações de validação do `ProdutoRequest` **antes** de executar o método. Se algo falhar, o método nem roda.

### Escolhendo o status da resposta

Quando o método devolve um objeto comum (`ProdutoResponse`, `List<...>`), o status é **200 OK**. Para outros status, usamos **`ResponseEntity`**, que representa a resposta inteira: status, cabeçalhos e corpo.

**Criar → 201 Created + `Location`:**
```java
URI location = ServletUriComponentsBuilder.fromCurrentRequest()  // http://localhost:8080/produtos
        .path("/{id}")                                           // + /{id}
        .buildAndExpand(criado.getId())                          // {id} → 1
        .toUri();                                                // http://localhost:8080/produtos/1
return ResponseEntity.created(location).body(ProdutoResponse.de(criado));
```
`201 Created` é o status específico para "criei algo novo". O cabeçalho **`Location`** informa **onde** o recurso criado pode ser consultado. É uma convenção do HTTP para `POST`.

**Remover → 204 No Content:**
```java
return ResponseEntity.noContent().build();
```
`204` significa "deu certo, e não há nada para devolver no corpo". Depois de apagar, não há produto para mostrar.

## 5. DTOs: por que não usar a entidade direto?

**DTO** (*Data Transfer Object*) é uma classe feita só para **transportar dados** entre o cliente e a API. Usamos dois:

```java
public record ProdutoRequest(String nome, String descricao, String categoria) { ... }    // entrada, sem id
public record ProdutoResponse(Long id, String nome, String descricao, String categoria) { ... } // saída
```

Por que não receber e devolver a entidade `Produto` direto?

| Problema de usar a entidade | Como o DTO resolve |
|---|---|
| O cliente poderia enviar `"id": 5` e tentar mexer em outro produto | `ProdutoRequest` **não tem** o campo `id` |
| Toda mudança no banco mudaria o JSON da API e quebraria o front-end | A API só muda quando mudamos o DTO de propósito |
| Campos internos vazariam (no futuro, `senha_hash` de Usuario!) | O `Response` só expõe o que foi escolhido |
| Validações de entrada (`@NotBlank`) misturadas com o mapeamento do banco (`@Column`) | Cada classe tem uma responsabilidade |

### O que é um `record`

`record` é um tipo do Java (desde a versão 16) para classes que **só carregam dados** e são **imutáveis**, ou seja, não mudam depois de criadas. Uma linha:

```java
public record ProdutoResponse(Long id, String nome, String descricao, String categoria) { }
```

O Java gera sozinho o construtor, os métodos de acesso (`id()`, `nome()`... sem o prefixo `get`), `equals`, `hashCode` e `toString`. O Jackson sabe ler e escrever `record`s, então eles são ideais para DTOs.

### Conversões

- **`request.paraEntidade()`**: transforma o DTO de entrada num `Produto` para o service.
- **`ProdutoResponse.de(produto)`**: transforma a entidade no DTO de saída. É um **método de fábrica** (*factory method*), um método estático que cria objetos.
- Na listagem, `.stream().map(ProdutoResponse::de).toList()` aplica a conversão a cada item da lista.

## 6. Validação: duas barreiras

### Barreira 1: Bean Validation, no DTO

```java
@NotBlank(message = "é obrigatório")
@Size(max = 150, message = "deve ter no máximo 150 caracteres")
String nome,
```

**Bean Validation** (pacote `jakarta.validation`) é uma especificação para declarar regras de validação com anotações. Vem do starter `spring-boot-starter-validation`, que já estava no `pom.xml`.

| Anotação | Regra |
|---|---|
| `@NotBlank` | Não pode ser `null`, vazio (`""`) nem só espaços (`"   "`) |
| `@Size(max = 150)` | No máximo 150 caracteres |

Os limites de `@Size` (150, 1000, 80) são **os mesmos** do `@Column(length = ...)` da entidade. Assim, um texto grande demais é recusado com uma mensagem clara (`400`), em vez de estourar como erro do banco (`500`).

### Barreira 2: regras do service

O service também recusa nome em branco (`produto-service.md`, seção 3). Por que validar duas vezes?
- O **DTO** protege a **entrada HTTP**, com mensagens por campo, pensadas para quem chama a API.
- O **service** protege a **regra de negócio** contra **qualquer** porta de entrada, inclusive as que ainda não existem, como uma importação de produtos de uma loja.

## 7. Tratamento de erros: `TratadorDeErros`

### O problema

Sem tratamento, uma `ProdutoNaoEncontradoException` lançada pelo service viraria **500 Internal Server Error**, o que dá a entender que "o servidor quebrou", quando na verdade o cliente pediu algo que não existe.

### A solução: `@RestControllerAdvice`

```java
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(ProdutoNaoEncontradoException ex) { ... 404 ... }
```

- **`@RestControllerAdvice`**: uma classe que "vigia" **todos** os controllers. Fica no pacote `erro/` porque não pertence só a Produto; Loja e Oferta (v0.2) vão usá-la também.
- **`@ExceptionHandler(X.class)`**: "quando qualquer controller lançar X, execute este método para montar a resposta".

| Exceção | Origem | Status |
|---|---|---|
| `ProdutoNaoEncontradoException` | Service: id inexistente | `404 Not Found` |
| `IllegalArgumentException` | Service: regra de negócio violada | `400 Bad Request` |
| `MethodArgumentNotValidException` | Spring: `@Valid` falhou | `400 Bad Request` + lista de campos |

### O formato: Problem Details (RFC 9457)

**RFC** é um documento de padrão da internet. A **RFC 9457** define um formato padrão de JSON para erros em APIs HTTP, identificado pelo `Content-Type: application/problem+json`. O Spring o implementa na classe **`ProblemDetail`**.

```json
{
  "title": "Recurso não encontrado",
  "status": 404,
  "detail": "Produto não encontrado: id 9999",
  "instance": "/produtos/9999"
}
```

| Campo | Significado |
|---|---|
| `title` | Resumo curto do **tipo** de erro |
| `status` | O código HTTP, repetido no corpo |
| `detail` | Explicação **desta** ocorrência específica |
| `instance` | A URL que causou o erro |
| `erros` | Campo **extra** nosso, só nos erros de validação: `{ "campo": "mensagem" }` |

Por que um padrão? O front-end pode tratar **todos** os erros da mesma forma, sempre lendo `title` e `detail`, sem um formato diferente para cada caso.

### As duas configurações novas no `application.properties`

```properties
spring.jpa.open-in-view=false
spring.mvc.problemdetails.enabled=true
```

- **`spring.mvc.problemdetails.enabled=true`**: erros que o **próprio Spring** trata, e não o `TratadorDeErros`, como JSON malformado, `/produtos/abc` ou método não suportado, também passam a usar o formato Problem Details. Sem isso, eles vinham num formato antigo (`timestamp`, `error`, `path`), inconsistente com o resto da API.
- **`spring.jpa.open-in-view=false`**: por padrão (`true`), o Spring mantém a conexão com o banco aberta **durante toda a requisição**, inclusive enquanto o JSON está sendo escrito. Isso esconde consultas acidentais fora do service e segura conexões por mais tempo. Com `false`, o acesso ao banco começa e termina nas transações do service (`@Transactional`). O aviso `spring.jpa.open-in-view is enabled by default`, que aparecia em todo log desde o início do projeto, sumiu.

## 8. Testes: `@WebMvcTest` e MockMvc

```java
@WebMvcTest(ProdutoController.class)
class ProdutoControllerTest {
    @Autowired    private MockMvc mvc;
    @MockitoBean  private ProdutoService service;
```

- **`@WebMvcTest`**: outro *slice test*, como o `@DataJpaTest` do repository. Sobe **só a camada web**: controllers, Jackson, Bean Validation e os `@RestControllerAdvice`. Sem banco, sem service real e sem servidor. No Spring Boot 4, fica em `org.springframework.boot.webmvc.test.autoconfigure`.
- **`MockMvc`**: simula requisições HTTP **em memória**, passando pelo mesmo caminho da seção 3 (DispatcherServlet, Jackson, validação...), mas sem abrir porta de rede.
- **`@MockitoBean`**: coloca um **mock** do `ProdutoService` no contexto do Spring. É o equivalente, num teste com Spring, ao `@Mock` do teste unitário do service. Fica no pacote `org.springframework.test.context.bean.override.mockito`. Em tutoriais antigos aparece o `@MockBean`, que foi removido.

Anatomia de um teste:

```java
when(service.buscarPorId(42L)).thenThrow(new ProdutoNaoEncontradoException(42L)); // 1. prepara o mock

mvc.perform(get("/produtos/42"))                                   // 2. faz a requisição
        .andExpect(status().isNotFound())                          // 3. confere o status
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.detail").value("Produto não encontrado: id 42"));  // e o JSON
```

**`jsonPath`** navega no JSON da resposta: `$` é a raiz, `$.detail` é o campo `detail`, `$[0].nome` é o `nome` do primeiro item de uma lista e `$.length()` é o tamanho da lista.

Os 7 testes cobrem: listar com filtro (200), buscar inexistente (404 + Problem Details), criar (201 + `Location`), criar sem nome (400 com `erros.nome`, **sem** chamar o service), atualizar (200), remover (204) e remover inexistente (404).

Com isso, o projeto tem **17 testes**, divididos em 4 tipos:

| Teste | Tipo | Sobe | Prova |
|---|---|---|---|
| `BackendApplicationTests` | Contexto completo | Tudo + H2 | A aplicação inicia |
| `ProdutoRepositoryTest` | Fatia JPA | JPA + H2 | Consultas no banco |
| `ProdutoServiceTest` | Unitário | Nada | Regras de negócio |
| `ProdutoServiceIntegracaoTest` | Fatia JPA + service | JPA + H2 + service | Dirty checking no banco |
| `ProdutoControllerTest` | Fatia web | Web + mock | Rotas, JSON, status, validação, erros |

---

# Parte 2: como usar

## 9. Subindo a API

**Opção A: tudo no Docker**, o mais simples. Na raiz do projeto:
```bash
docker compose up -d --build
docker compose logs -f backend     # espere "Started BackendApplication"; Ctrl+C sai do log
```

**Opção B: banco no Docker e API pela IDE ou pelo Maven**, para desenvolver:
```bash
docker compose up -d mysql
cd backend
./mvnw spring-boot:run             # Windows (cmd/PowerShell): mvnw.cmd spring-boot:run
```

Nos dois casos, a API fica em **`http://localhost:8080/produtos`**.

## 10. Referência dos endpoints

### `GET /produtos`: listar todos

```
GET /produtos
→ 200 OK
[
  {"id":1,"nome":"Notebook Dell","descricao":null,"categoria":"Informática"},
  {"id":2,"nome":"Mouse sem fio","descricao":null,"categoria":"Periféricos"}
]
```
Sem produtos, devolve `[]`, uma lista vazia e não um erro.

### `GET /produtos?nome={texto}`: pesquisar por parte do nome

Não diferencia maiúsculas de minúsculas e encontra o texto em qualquer posição do nome.
```
GET /produtos?nome=NOTE
→ 200 OK
[{"id":1,"nome":"Notebook Dell","descricao":null,"categoria":"Informática"}]
```

### `GET /produtos/{id}`: buscar um produto

```
GET /produtos/1    → 200 OK      {"id":1,"nome":"Notebook Dell",...}
GET /produtos/999  → 404 Not Found (Problem Details)
GET /produtos/abc  → 400 Bad Request (id não numérico)
```

### `POST /produtos`: criar

Corpo (JSON):

| Campo | Obrigatório | Limite |
|---|---|---|
| `nome` | Sim (não pode ser vazio nem só espaços) | 150 caracteres |
| `descricao` | Não | 1000 caracteres |
| `categoria` | Não | 80 caracteres |

```
POST /produtos
Content-Type: application/json

{"nome":"  Notebook Dell  ","categoria":"Informática"}

→ 201 Created
Location: http://localhost:8080/produtos/1
{"id":1,"nome":"Notebook Dell","descricao":null,"categoria":"Informática"}
```
Os espaços das pontas do nome foram removidos pelo service. Um `id` enviado no corpo é ignorado.

### `PUT /produtos/{id}`: atualizar

**Substitui** todos os campos editáveis. Um campo omitido vira `null`, e o nome continua obrigatório.
```
PUT /produtos/2
Content-Type: application/json

{"nome":"Mouse gamer","categoria":"Periféricos"}

→ 200 OK
{"id":2,"nome":"Mouse gamer","descricao":null,"categoria":"Periféricos"}
```
Respostas de erro: `404` (id inexistente) ou `400` (dados inválidos).

### `DELETE /produtos/{id}`: remover

```
DELETE /produtos/2  → 204 No Content   (sem corpo)
DELETE /produtos/2  → 404 Not Found     (já foi removido)
```

### Resumo dos status

| Status | Quando |
|---|---|
| `200 OK` | Consulta ou atualização bem-sucedida |
| `201 Created` | Produto criado (veja o cabeçalho `Location`) |
| `204 No Content` | Produto removido |
| `400 Bad Request` | Dados inválidos, JSON malformado ou id não numérico |
| `404 Not Found` | Produto inexistente |

### Exemplos de respostas de erro

Validação (`POST` com `"nome": ""`):
```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos são inválidos.",
  "instance": "/produtos",
  "erros": { "nome": "é obrigatório" }
}
```

JSON malformado:
```json
{ "title": "Bad Request", "status": 400, "detail": "Failed to read request", "instance": "/produtos" }
```

## 11. Testando na prática

### No navegador (só `GET`)

O navegador faz `GET` quando você digita uma URL. Abra:
- `http://localhost:8080/produtos`
- `http://localhost:8080/produtos?nome=note`
- `http://localhost:8080/produtos/1`

Para `POST`, `PUT` e `DELETE`, use uma das opções abaixo ou um programa visual como o **Postman** ou o **Insomnia**.

### Com `curl` no Git Bash

**`curl`** é um programa de linha de comando que faz requisições HTTP.

| Opção | Significado |
|---|---|
| `-i` | Mostra também o status e os cabeçalhos da resposta |
| `-X POST` | Escolhe o método HTTP (o padrão é `GET`) |
| `-H "..."` | Adiciona um cabeçalho |
| `-d '...'` | Envia um corpo (texto direto no comando) |
| `--data-binary @arquivo.json` | Envia o conteúdo de um arquivo como corpo, sem alterar nenhum byte |

```bash
# listar e pesquisar
curl -i http://localhost:8080/produtos
curl "http://localhost:8080/produtos?nome=note"

# criar (sem acentos, pode ir direto no comando)
curl -i -X POST http://localhost:8080/produtos \
     -H "Content-Type: application/json" \
     -d '{"nome":"Mouse sem fio","categoria":"Perifericos"}'

# atualizar o produto 1
curl -i -X PUT http://localhost:8080/produtos/1 \
     -H "Content-Type: application/json" \
     -d '{"nome":"Mouse gamer"}'

# remover o produto 1
curl -i -X DELETE http://localhost:8080/produtos/1
```

O `\` no fim da linha só continua o comando na linha de baixo.

### ⚠️ Acentos no Windows

O JSON **exige** a codificação **UTF-8**, a tabela que define quais bytes representam cada letra. Ao testar, aparecem dois problemas diferentes:

**1. Enviando com `curl -d` no Git Bash.** Com acentos digitados direto no comando, o terminal do Windows pode enviar os bytes em outra codificação. A API responde `400`, e o log do backend mostra `Invalid UTF-8 middle byte`. A solução é colocar o JSON num **arquivo salvo em UTF-8**, que é o padrão do VS Code, e enviar com `--data-binary`:
```bash
# produto.json contém: {"nome":"Cadeira ergonômica","categoria":"Escritório"}
curl -i -X POST http://localhost:8080/produtos \
     -H "Content-Type: application/json" \
     --data-binary @produto.json
```

**2. Lendo com o PowerShell 5.1** (o PowerShell que vem no Windows). O envio funciona, mas **a leitura mostra "EscritÃ³rio"**. Os dados estão corretos no banco. O problema é que o PowerShell 5.1 não presume UTF-8 ao ler a resposta, porque a API responde `Content-Type: application/json` sem dizer `charset`, já que o padrão do JSON é UTF-8.

### Com PowerShell

```powershell
# listar (acentos podem aparecer errados na exibição; veja abaixo)
Invoke-RestMethod http://localhost:8080/produtos

# criar: converta o texto para bytes UTF-8 antes de enviar
$json = '{"nome":"Cadeira ergonômica","categoria":"Escritório"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/produtos `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($json))

# ler com acentos corretos: pegue os bytes da resposta e decodifique como UTF-8
$r = Invoke-WebRequest -UseBasicParsing http://localhost:8080/produtos
[System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json

# remover
Invoke-RestMethod -Method Delete http://localhost:8080/produtos/1
```

- `Invoke-RestMethod`: faz a requisição e já converte o JSON da resposta em objetos do PowerShell.
- `Invoke-WebRequest`: faz a requisição e devolve a resposta "crua", o que permite decodificar os bytes manualmente.
- `[System.Text.Encoding]::UTF8.GetBytes(...)` / `GetString(...)`: convertem texto em bytes UTF-8 e vice-versa.
- O `` ` `` (crase) no fim da linha continua o comando na linha de baixo, como o `\` no Bash.

> O PowerShell 7 (`pwsh`), que pode ser instalado à parte, já usa UTF-8 por padrão e não tem esse problema.

## 12. Problemas comuns

| Sintoma | Causa | Solução |
|---|---|---|
| `curl: (7) Failed to connect to localhost port 8080` | API não está rodando | `docker compose up -d` e aguardar `Started BackendApplication` |
| `400` com acentos e log `Invalid UTF-8 middle byte` | Corpo não está em UTF-8 | Enviar de um arquivo UTF-8 (`--data-binary @arquivo.json`) |
| Acentos aparecem como `Ã¡`, `Ã³` | Programa lendo a resposta como ISO-8859-1 | Os dados estão certos; decodificar como UTF-8 (seção 11) |
| `415 Unsupported Media Type` | Faltou o cabeçalho `Content-Type: application/json` | Adicionar `-H "Content-Type: application/json"` |
| `404` em uma URL que deveria existir | Erro de digitação na rota (ex.: `/produto` sem "s") | Conferir a URL; as rotas começam com `/produtos` |
| `405 Method Not Allowed` | Método errado para a rota (ex.: `PUT /produtos` sem id) | Conferir a tabela da seção 10 |
| Código novo não aparece no Docker | Imagem antiga | `docker compose up -d --build` |

## 13. Rodando os testes

```bash
cd backend
./mvnw test                               # todos os 17 testes
./mvnw test -Dtest=ProdutoControllerTest  # só os 7 do controller
```

## 14. Próximos passos

Com o controller, o **CRUD de Produto da v0.1 está completo**. CRUD vem de *Create, Read, Update, Delete*: criar, ler, atualizar e remover. Caminhos possíveis:

- Abrir um **Pull Request** de `feature/v0.1-backend` para `desenvolvimento`.
- **v0.2** (`modelo-dados.md`): entidades **Loja** e **Oferta**, com o primeiro **relacionamento** entre tabelas (uma oferta pertence a um produto e a uma loja).
- Melhorias opcionais: paginação na listagem (`?page=0&size=20`) e documentação interativa da API com **Swagger/OpenAPI**.
