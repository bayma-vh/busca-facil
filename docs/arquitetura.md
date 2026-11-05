# Arquitetura

Visão estrutural do Busca Fácil: como o sistema é dividido, como as partes se comunicam e quais convenções o código segue. Para a explicação detalhada de cada componente, veja os documentos específicos citados em cada seção.

---

## 1. Visão geral do sistema

```
┌─────────────┐      HTTP/JSON       ┌──────────────────────┐       JDBC        ┌──────────────┐
│  Cliente    │ ───────────────────► │  Back-end            │ ────────────────► │  MySQL 8     │
│ (front-end, │ ◄─────────────────── │  Spring Boot (Java)  │ ◄──────────────── │  busca_facil │
│  curl, etc.)│     porta 8080       │  container Docker    │    porta 3306     │  container   │
└─────────────┘                      └──────────────────────┘                   └──────────────┘
                                                                                volume mysql_data
```

| Parte | Tecnologia | Situação |
|---|---|---|
| Front-end | Next.js + React + TypeScript | Planejado (pasta `frontend/` vazia) |
| Back-end | Java 25 + Spring Boot 4.1 | v0.1 concluída |
| Banco de dados | MySQL 8 | Em uso, via Docker |

O front-end e o back-end são **aplicações separadas** que conversam apenas por HTTP. O back-end não gera páginas HTML: ele expõe uma **API REST** que devolve JSON. Assim, o mesmo back-end pode servir um site, um aplicativo de celular ou outro sistema.

---

## 2. Arquitetura em camadas do back-end

O back-end segue a **arquitetura em camadas**: cada camada tem uma responsabilidade e só conversa com a camada imediatamente abaixo.

```
            ┌───────────────────────────────────────────────┐
  HTTP ───► │ Controller   traduz HTTP ⇄ Java               │  ProdutoController
            │              valida entrada (@Valid + DTO)    │  ProdutoRequest / ProdutoResponse
            ├───────────────────────────────────────────────┤
            │ Service      regras de negócio                │  ProdutoService
            │              transações (@Transactional)      │
            ├───────────────────────────────────────────────┤
            │ Repository   acesso a dados (Spring Data JPA) │  ProdutoRepository
            ├───────────────────────────────────────────────┤
            │ Entidade     mapeamento classe ⇄ tabela (JPA) │  Produto
            └───────────────────────────────────────────────┘
                                   │ Hibernate (SQL)
                                   ▼
                                 MySQL

  Transversal: TratadorDeErros (@RestControllerAdvice) converte exceções em respostas HTTP.
```

| Camada | Pode depender de | Nunca deve |
|---|---|---|
| Controller | Service, DTOs | Acessar o repository direto; conter regras de negócio |
| Service | Repository, entidades | Conhecer HTTP (status, cabeçalhos, JSON) |
| Repository | Entidades | Conter regras de negócio |
| Entidade | Nada | Ser exposta diretamente na API |

**Por que camadas?**
- **Cada mudança tem um lugar óbvio.** Mudou uma rota: controller. Mudou uma regra: service. Mudou uma consulta: repository.
- **As regras valem para qualquer entrada.** Uma importação automática de produtos usaria o mesmo service do controller.
- **Testes isolados.** Cada camada pode ser testada substituindo a de baixo por um mock.

Documentos: `entidade-produto.md`, `produto-repository.md`, `produto-service.md`, `produto-controller.md`.

---

## 3. Organização do código: por funcionalidade

```
backend/src/main/java/com/busca_facil/backend/
├── BackendApplication.java        ponto de entrada (@SpringBootApplication)
├── erro/
│   └── TratadorDeErros.java       tratamento de erros compartilhado por todas as funcionalidades
└── produto/                       tudo sobre Produto
    ├── Produto.java               entidade
    ├── ProdutoRepository.java     repository
    ├── ProdutoService.java        service
    ├── ProdutoNaoEncontradoException.java
    ├── ProdutoController.java     controller
    ├── ProdutoRequest.java        DTO de entrada
    └── ProdutoResponse.java       DTO de saída
```

O código é agrupado **por funcionalidade** (*package by feature*), e não por tipo técnico (`controller/`, `service/`...). Quando chegarem Loja e Oferta (v0.2), cada uma terá seu próprio pacote (`loja/`, `oferta/`) com a mesma estrutura interna.

Todos os pacotes ficam **abaixo** de `com.busca_facil.backend`. É o que permite ao `@SpringBootApplication` encontrar automaticamente entidades, repositories, services e controllers.

---

## 4. Fluxo de uma requisição

Exemplo: `POST /produtos` com `{"nome":"Notebook"}`.

```
Tomcat (porta 8080)
  → DispatcherServlet escolhe ProdutoController.criar
  → Jackson: JSON → ProdutoRequest
  → Bean Validation (@NotBlank, @Size)       ✗ → 400 (TratadorDeErros)
  → ProdutoService.criar                     ✗ regra violada → 400
      [abre transação]
      → ProdutoRepository.save
          → Hibernate: INSERT INTO produto ...
      [commit]
  → ProdutoResponse.de(produto)
  → Jackson: ProdutoResponse → JSON
  → 201 Created + Location: /produtos/1
```

---

## 5. Contrato de erros

Todos os erros da API usam o formato **Problem Details** (RFC 9457), com `Content-Type: application/problem+json`:

```json
{ "title": "...", "status": 404, "detail": "...", "instance": "/produtos/9" }
```

| Situação | Status |
|---|---|
| Recurso inexistente | `404` |
| Regra de negócio violada | `400` |
| Validação de campos (traz o mapa `erros`) | `400` |
| JSON malformado, id não numérico (erros do próprio Spring) | `400` |

---

## 6. Configuração e ambientes

```
               .env (senhas; fora do Git)
                │                     │
     docker-compose.yml        application.properties (spring.config.import)
                │                     │
     ┌──────────┴──────┐     ┌────────┴─────────────────┐
     │ Dentro do Docker │     │ Fora do Docker (IDE/mvnw) │
     │ mysql:3306       │     │ localhost:3307            │
     └──────────────────┘     └───────────────────────────┘

     Testes: src/test/resources/application.properties → H2 em memória (sem MySQL)
```

| Ambiente | Banco | Origem da URL |
|---|---|---|
| Docker Compose | MySQL (`mysql:3306`) | Variável `SPRING_DATASOURCE_URL` do compose |
| IDE / `./mvnw spring-boot:run` | MySQL do Docker (`localhost:3307`) | Valor padrão no `application.properties` |
| `./mvnw test` | H2 em memória | `application.properties` de teste |

Detalhes: `docker-banco-e-testes.md`.

---

## 7. Estratégia de testes

```
         ┌───────────────────────┐
         │ Contexto completo (1) │  a aplicação inicia?
         ├───────────────────────┤
         │ Fatias (10)           │  @WebMvcTest (web), @DataJpaTest (banco)
         ├───────────────────────┤
         │ Unitários (6)         │  Mockito, sem Spring: regras do service
         └───────────────────────┘
```

Muitos testes pequenos e rápidos na base; poucos testes amplos no topo. Todos rodam sem Docker.

---

## 8. Decisões e próximos passos

| Decisão | Motivo | Quando revisar |
|---|---|---|
| `ddl-auto=update` (o Hibernate cria as tabelas) | Simples para estudo | Ao entrar em produção: migrar para **Flyway** |
| H2 nos testes | Testes rodam em qualquer máquina | Se surgir SQL específico do MySQL: **Testcontainers** |
| DTOs separados da entidade | Contrato da API estável; sem vazar campos | Mantida |
| Regras no service, não no controller | Reuso e testes isolados | Mantida |
| `open-in-view=false` | Acesso ao banco só nas transações do service | Mantida |

Próximas versões (`modelo-dados.md`): **v0.2** Loja e Oferta (primeiros relacionamentos `@ManyToOne`/`@OneToMany`), **v0.3** Usuario, **v0.4** Favorito.
