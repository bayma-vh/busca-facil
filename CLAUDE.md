# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Regras do usuário (prioridade máxima)

- **Responda sempre em português (Brasil).**
- **Projeto de estudo: explique tudo.** Ao usar ou mencionar qualquer termo, ferramenta, tecnologia ou comando, explique **o que é**, **como funciona** e **por que usar** neste contexto. Isso vale também para cada comando executado no terminal.
- **Nunca faça `git push` sem pedido explícito.** Commits seguem o fluxo abaixo.

## Fluxo de trabalho por componente

Quando o usuário pedir o próximo componente, siga este modelo:
1. **Implementar** o componente com seus testes; rodar `./mvnw test` e, quando fizer sentido, testar no Docker (`docker compose up -d --build` + requisições reais).
2. **Documentar** em `docs/<componente>.md`, em linguagem clara e detalhada: como funciona (conceitos, anotações, fluxo) e como usar (exemplos reais, saídas observadas, problemas comuns). Atualizar este CLAUDE.md se o estado do projeto mudar.
3. **Commitar** (autorizado nesse fluxo): um commit por componente, código + testes + documento juntos, mensagem Conventional Commits em português (`feat:`, `docs:`, `chore:`), coerente com a branch atual. Mudanças pendentes de outro assunto vão em commit separado. **Não fazer push** sem pedido.

## Sobre o projeto

Busca Fácil: plataforma de busca e comparação de preços de produtos entre lojas. É um projeto de estudo (APIs REST, banco de dados, arquitetura). Fase atual: **v0.1, somente backend, CRUD de Produto completo** (pacote `produto/`): entidade, repository, service, controller REST em `/produtos` e DTOs (`record`s `ProdutoRequest`/`ProdutoResponse`). Próximo marco: PR para `desenvolvimento` e depois v0.2 (Loja, Oferta).

Arquitetura em camadas: Controller (HTTP ↔ DTO, `@Valid`) → Service (regras de negócio, `@Transactional`, injeção por construtor; `atualizar` usa dirty checking, sem `save`) → Repository (Spring Data JPA). Exceções de negócio são `RuntimeException` (ex.: `ProdutoNaoEncontradoException`, `IllegalArgumentException`) e viram HTTP no `erro/TratadorDeErros` (`@RestControllerAdvice`, formato `ProblemDetail`/RFC 9457: 404, 400, 400 com mapa `erros` por campo). Nunca expor a entidade na API; usar DTOs. Organização por funcionalidade: cada entidade tem seu pacote; o pacote `erro/` é compartilhado.

Padrão de testes: unitário do service com Mockito (`@ExtendWith(MockitoExtension.class)`, sem Spring); repository e integração com `@DataJpaTest` (+ `@Import` do service); controller com `@WebMvcTest` + `MockMvc` + `@MockitoBean`. No Boot 4 os pacotes mudaram: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, `org.springframework.boot.jpa.test.autoconfigure.TestEntityManager`, `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, `org.springframework.test.context.bean.override.mockito.MockitoBean` (o antigo `@MockBean` não existe).

- `backend/`: API em Java 25 + Spring Boot 4.1 (Web MVC, Data JPA, Validation, Lombok), pacote base `com.busca_facil.backend`.
- `frontend/`: vazio por enquanto. O plano é Next.js + React + TypeScript.
- `docs/modelo-dados.md`: modelo de dados planejado e em qual versão cada entidade entra. Produto (v0.1), Loja e Oferta (v0.2), Usuario (v0.3), Favorito (v0.4). Siga esse roteiro ao criar entidades.
- O README cita PostgreSQL, mas **o banco usado de fato é MySQL 8** (docker-compose + `mysql-connector-j`).

## Fluxo de branches

- Branch principal / alvo de PRs: `desenvolvimento`.
- Trabalho atual: `feature/v0.1-backend`, uma branch por versão.

## Comandos

Os comandos Maven rodam dentro de `backend/`, pelo Maven Wrapper. No Windows, use `mvnw.cmd`.

```bash
cd backend
./mvnw spring-boot:run                      # roda a API local (porta 8080)
./mvnw clean package                        # compila e gera o .jar em target/
./mvnw test                                 # roda todos os testes
./mvnw test -Dtest=BackendApplicationTests  # roda uma classe de teste
./mvnw test -Dtest=BackendApplicationTests#contextLoads  # roda um único método
```

Docker (na raiz do repositório):

```bash
docker compose up -d mysql        # sobe só o MySQL (para rodar a API pela IDE ou pelo mvnw)
docker compose up -d --build      # sobe MySQL + backend em containers
docker compose down               # para tudo (o volume mysql_data guarda os dados)
```

Não há linter configurado.

Particularidades deste ambiente (Windows):
- O Maven pode falhar ao baixar plugins/dependências (`PKIX path building failed`, certificado). Se o necessário já está em `~/.m2`, use `./mvnw -o test` (offline).
- Ao testar a API com `curl -d` no Git Bash, acentos chegam fora de UTF-8 (`Invalid UTF-8 middle byte` → 400). Envie o corpo de um arquivo UTF-8 com `--data-binary @arquivo.json`. O PowerShell 5.1 exibe respostas com acento errado (`Ã¡`), mas os dados estão corretos.

## Configuração e banco de dados (como as peças se conectam)

- As credenciais ficam no `.env` da raiz, que o Git ignora: `MYSQL_ROOT_PASSWORD`, `DB_USER`, `DB_PASSWORD`. O `docker-compose.yml` lê esse arquivo.
- `application.properties` **também importa o `.env`** via `spring.config.import`, tentando `.env` e `../.env`, porque a aplicação pode rodar a partir da raiz ou de `backend/`. Assim a API rodando fora do Docker usa as mesmas credenciais.
- A URL do banco tem dois modos:
  - **Fora do Docker:** padrão `localhost:3307`, a porta do MySQL do compose exposta no host.
  - **Dentro do Docker:** o compose define `SPRING_DATASOURCE_URL` apontando para `mysql:3306`, e essa variável sobrescreve o padrão.
- `spring.jpa.hibernate.ddl-auto=update`: o Hibernate cria e altera as tabelas a partir das classes `@Entity`. Não há migrations (Flyway/Liquibase).
- `spring.jpa.open-in-view=false` (acesso ao banco só dentro das transações do service) e `spring.mvc.problemdetails.enabled=true` (erros padrão do Spring também em Problem Details).
- Testes **não usam o MySQL**: `backend/src/test/resources/application.properties` substitui o principal (vem antes no classpath) e aponta para o H2 em memória (`MODE=MySQL`, `ddl-auto=create-drop`). O `.env` não é importado nos testes. `./mvnw test` funciona sem Docker.
- O `backend/Dockerfile` usa build multi-stage (Maven + Temurin 25 → JRE 25). Ao mudar `java.version` no `pom.xml`, atualize também as imagens do Dockerfile.
