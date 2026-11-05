# Guia: README, Docker e próximo passo (05/11/2026)

## 1. Estado atual da branch `feature/v0.1-backend`

- Esqueleto Spring Boot 4.1.1 + Java 25 em backend/, com a entidade Produto, repository, service, controller e testes (v0.1 concluída).
- `docker-compose.yml` sobe MySQL 8.0 (porta 3307 no seu PC) e o back-end (porta 8080).
- O `dockerfile` antigo da raiz **já foi removido** no commit `90e73e7`, então isso está resolvido.
- Rodei `./mvnw test` aqui: o teste `contextLoads` **falha** com
  `Failed to configure a DataSource: 'url' attribute is not specified`. Ou seja, fora do Docker a aplicação não sabe onde está o banco.

## 2. O Docker está atualizado?

**Está funcional, mas com quatro ajustes recomendados.**

O que já está bom:
- **Build multi-stage** no `backend/Dockerfile`: a etapa 1 usa uma imagem com Maven para compilar; a etapa 2 usa só o JRE (o "executor" do Java) para rodar. A imagem final fica menor porque não leva o Maven nem o código-fonte.
- **Healthcheck + `depends_on: condition: service_healthy`**: o back-end só sobe depois que o MySQL responde. Sem isso, o Spring tentaria conectar num banco que ainda está inicializando e cairia.
- `.env` fora do Git e `.dockerignore` evitando mandar `target/` e `.git` para dentro da imagem.

### Ajuste A: healthcheck com `127.0.0.1` em vez de `localhost`
Na primeira vez que o container do MySQL sobe, ele roda um servidor *temporário* para criar o banco e o usuário. Esse servidor temporário aceita conexões só pelo *socket* (um arquivo local), não pela rede. Com `-h localhost`, o `mysqladmin` usa o socket e responde "ok" cedo demais; com `-h 127.0.0.1` ele usa TCP (rede) e só responde quando o servidor definitivo está no ar.

```yaml
test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1"]
```

### Ajuste B: configurar o banco no `application.properties`
Hoje o arquivo só tem o nome da aplicação. Sugestão:

```properties
spring.application.name=backend

# ${VARIAVEL:valor-padrao} -> usa a variável de ambiente se existir; senão, o padrão.
# No Docker, o compose define SPRING_DATASOURCE_URL; no seu PC, vale o padrão (porta 3307).
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3307/busca_facil?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:${DB_USER:}}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:${DB_PASSWORD:}}

# update = o Hibernate cria/ajusta as tabelas a partir das entidades. Ótimo para estudo;
# em produção usaríamos uma ferramenta de migração (Flyway), que veremos depois.
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Assim você consegue rodar o MySQL no Docker (`docker compose up mysql`) e o back-end pela sua IDE.

### Ajuste C: testes que não dependem do MySQL
O **H2** é um banco que roda dentro da memória do Java, criado e destruído junto com o teste. Usá-lo nos testes faz `./mvnw test` funcionar em qualquer máquina.

No `pom.xml`, dentro de `<dependencies>`:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope> <!-- só existe durante os testes, não vai para o .jar final -->
</dependency>
```

E crie `backend/src/test/resources/application.properties` (ele substitui o principal durante os testes):
```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
```

Depois disso, `./mvnw test` deve passar. O `-DskipTests` do Dockerfile pode continuar, para o build da imagem ser rápido.

### Ajuste D (opcional, boa prática): não rodar como root
No final do `Dockerfile`, antes do `ENTRYPOINT`, adicionar `USER 1000`. Se alguém explorar uma falha na aplicação, ela não terá poderes de administrador dentro do container.

## 3. Como aplicar o README novo (comandos git explicados)

O README atualizado está em `README.md` ao lado deste guia. No terminal, na pasta do projeto:

```bash
git status
```
Mostra em que branch você está e quais arquivos mudaram. Sempre rode antes de qualquer coisa, para não trabalhar na branch errada.

```bash
git switch feature/v0.1-backend
git pull origin feature/v0.1-backend
```
`git switch` troca de branch (é a versão moderna e mais clara do `git checkout <branch>`). `git pull` baixa os commits do GitHub e junta com a sua cópia local, para você partir da versão mais recente.

Copie o `README.md` novo por cima do antigo, na raiz do projeto, e então:

```bash
git diff README.md
```
Mostra linha a linha o que mudou (`-` vermelho saiu, `+` verde entrou). Serve para revisar antes de gravar.

```bash
git add README.md
git commit -m "docs: atualiza README com stack real (MySQL), Docker e roadmap"
```
`git add` coloca o arquivo na *staging area* (a "caixa" do que vai entrar no próximo commit). `git commit` grava essa caixa no histórico com uma mensagem. O prefixo `docs:` segue o padrão *Conventional Commits* que você já usa (`chore:`, `docs:`, `feat:`), e diz o tipo da mudança.

```bash
git push origin feature/v0.1-backend
```
Envia seus commits locais para a branch no GitHub.

Sugestão: faça os ajustes A, B e C num segundo commit separado, por exemplo `chore: configura datasource e testes com H2`. Commits pequenos e com um único assunto deixam o histórico fácil de entender e de desfazer.

## 4. Próximo passo lógico: entidade Produto (v0.1)

Pelo `docs/modelo-dados.md`, a v0.1 é **Produto (id, nome, descrição, categoria)**. Vamos construir em camadas, uma por vez:

| Camada | O que é | Arquivo |
|---|---|---|
| Entidade | Classe Java que vira uma tabela no banco | `produto/Produto.java` |
| Repository | Interface que faz o acesso ao banco (salvar, buscar...) sem escrever SQL | `produto/ProdutoRepository.java` |
| Service | Regras de negócio (ex.: não aceitar nome vazio) | `produto/ProdutoService.java` |
| Controller | Recebe as requisições HTTP (`GET /produtos`, `POST /produtos`) | `produto/ProdutoController.java` |

**Passo 4.1, a entidade.** Crie `backend/src/main/java/com/busca_facil/backend/produto/Produto.java`:

```java
package com.busca_facil.backend.produto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity                       // JPA: esta classe é mapeada para uma tabela
@Table(name = "produto")      // nome da tabela no MySQL
@Getter @Setter               // Lombok: gera getters e setters na compilação
@NoArgsConstructor            // JPA exige um construtor sem argumentos
public class Produto {

    @Id                                                   // chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // o MySQL gera o id (AUTO_INCREMENT)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(length = 80)
    private String categoria;
}
```

Por que um pacote `produto/`? É a organização **por funcionalidade**: tudo de Produto fica junto, e quando chegarem Loja e Oferta cada uma terá seu pacote.

Para testar: com o ajuste B feito, rode `docker compose up --build` e veja no log o Hibernate executando `create table produto ...`. Quando funcionar, faça o commit `feat: adiciona entidade Produto` e me avise; seguimos para o Repository.
