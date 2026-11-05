# Busca Fácil

Plataforma de busca e comparação de preços de produtos.

## Objetivo

O Busca Fácil é um projeto de estudo desenvolvido com o objetivo de aplicar conceitos de desenvolvimento de software, desenvolvimento web, APIs REST, bancos de dados e arquitetura de sistemas.

A plataforma permitirá pesquisar produtos e comparar preços oferecidos por diferentes lojas.

## Tecnologias

### Back-end
- Java 25
- Spring Boot 4.1 (Spring Web MVC, Spring Data JPA, Bean Validation)
- Hibernate (JPA)
- Lombok
- Maven (via Maven Wrapper)

### Banco de dados
- MySQL 8 (em container Docker)
- H2 em memória (somente nos testes)

### Testes
- JUnit 5, Mockito, AssertJ, MockMvc

### Infraestrutura
- Docker e Docker Compose

### Front-end (planejado)
- Next.js
- React
- TypeScript

## Como rodar

Pré-requisitos: [Docker Desktop](https://www.docker.com/products/docker-desktop/). Para rodar fora do Docker ou executar os testes, também é preciso o JDK 25.

1. Crie o arquivo `.env` a partir do modelo e preencha as senhas:
   ```bash
   cp .env.example .env
   ```
2. Suba o banco e a API:
   ```bash
   docker compose up -d --build
   ```
3. A API fica disponível em `http://localhost:8080/produtos`.

Rodar os testes (não precisam do Docker):
```bash
cd backend
./mvnw test        # Windows (cmd/PowerShell): mvnw.cmd test
```

## API (v0.1)

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/produtos` | Lista todos os produtos |
| `GET` | `/produtos?nome=texto` | Pesquisa por parte do nome |
| `GET` | `/produtos/{id}` | Busca um produto |
| `POST` | `/produtos` | Cria um produto |
| `PUT` | `/produtos/{id}` | Atualiza um produto |
| `DELETE` | `/produtos/{id}` | Remove um produto |

Detalhes e exemplos em [`docs/produto-controller.md`](docs/produto-controller.md).

## Funcionalidades

- [x] Cadastro de produtos (v0.1)
- [x] Pesquisa de produtos (v0.1)
- [ ] Cadastro de lojas (v0.2)
- [ ] Cadastro de ofertas (v0.2)
- [ ] Comparação de preços
- [ ] Exibição do menor preço
- [ ] Histórico de preços
- [ ] Cadastro de usuários (v0.3)
- [ ] Favoritos (v0.4)

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/v0.1.md`](docs/v0.1.md) | **Visão completa da v0.1**: funcionalidades, componentes, tecnologias e como usar |
| [`docs/arquitetura.md`](docs/arquitetura.md) | Arquitetura em camadas e organização do código |
| [`docs/modelo-dados.md`](docs/modelo-dados.md) | Modelo de dados planejado por versão |
| [`docs/CHANGELOG.md`](docs/CHANGELOG.md) | Histórico de mudanças por versão |
| [`docs/docker-banco-e-testes.md`](docs/docker-banco-e-testes.md) | Docker, configuração do banco e testes com H2 |
| [`docs/entidade-produto.md`](docs/entidade-produto.md) | Entidade JPA `Produto` |
| [`docs/produto-repository.md`](docs/produto-repository.md) | `ProdutoRepository` e Spring Data JPA |
| [`docs/produto-service.md`](docs/produto-service.md) | `ProdutoService` e regras de negócio |
| [`docs/produto-controller.md`](docs/produto-controller.md) | `ProdutoController` e guia de uso da API |

## Status

🚧 Em desenvolvimento. **v0.1 (back-end de Produto) concluída.**
