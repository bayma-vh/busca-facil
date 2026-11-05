# Changelog

Todas as mudanças relevantes do projeto, organizadas por versão.

O formato segue a ideia do [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/): cada versão lista o que foi **Adicionado**, **Alterado**, **Corrigido** ou **Removido**. As mensagens de commit seguem o padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/) (`feat:`, `docs:`, `chore:`).

---

## [Não lançado]

### Planejado: v0.2
- Entidades **Loja** e **Oferta**, com relacionamentos (um produto tem várias ofertas; uma loja tem várias ofertas).
- Comparação de preços e exibição do menor preço.

---

## [v0.1] - 2026-11-05

Primeira versão funcional do back-end: CRUD de Produto exposto como API REST.

### Adicionado
- **Esqueleto Spring Boot** (Java 25, Spring Boot 4.1) em `backend/`.
- **Docker**: `backend/Dockerfile` multi-stage (Maven → JRE) e `docker-compose.yml` com MySQL 8 + back-end.
- **Configuração do banco** em `application.properties`: importa o `.env`, URL padrão para rodar fora do Docker (`localhost:3307`) e `ddl-auto=update`.
- **Testes com H2** em memória (`src/test/resources/application.properties`), sem depender do MySQL.
- **Entidade `Produto`** (id, nome, descrição, categoria) mapeada para a tabela `produto`.
- **`ProdutoRepository`** (Spring Data JPA) com a busca `findByNomeContainingIgnoreCase`.
- **`ProdutoService`** com as regras de negócio: nome obrigatório, `trim` nos textos, `criar` ignora o id recebido, `ProdutoNaoEncontradoException`.
- **`ProdutoController`**, API REST em `/produtos`: `GET` (lista e `?nome=`), `GET /{id}`, `POST` (201 + `Location`), `PUT /{id}`, `DELETE /{id}` (204).
- **DTOs** `ProdutoRequest` (com Bean Validation) e `ProdutoResponse`.
- **`TratadorDeErros`** (`@RestControllerAdvice`): erros no formato Problem Details (RFC 9457): 404, 400 e erros de validação por campo.
- **17 testes automatizados**: contexto, repository, service (unitário e integração) e controller.
- **Documentação** em `docs/` para cada componente, mais o `CLAUDE.md`.

### Alterado
- Healthcheck do MySQL usa `127.0.0.1` (TCP) em vez de `localhost` (socket), para só indicar "pronto" quando o servidor definitivo estiver no ar.
- Container do back-end roda como usuário comum (`USER 1000`), não como root.
- `spring.jpa.open-in-view=false` e `spring.mvc.problemdetails.enabled=true`.
- README atualizado: o banco real é **MySQL** (não PostgreSQL), instruções de execução e status das funcionalidades.

### Removido
- `dockerfile` antigo da raiz (compilava um `Main.java` com `javac`, de antes do projeto usar Spring Boot).
