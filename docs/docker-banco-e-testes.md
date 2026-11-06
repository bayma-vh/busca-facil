# Docker, banco de dados e testes: como tudo funciona

Este documento explica como o back-end do Busca Fácil se conecta ao banco, como os containers Docker são montados e por que os testes não precisam do MySQL. Ele cobre os ajustes A, B, C e D do `guia-passo-a-passo.md`.

---

## 1. Visão geral

```
                 ┌──────────────── seu PC (Windows) ────────────────┐
                 │                                                   │
  navegador ───► │ localhost:8080 ──► container busca-facil-backend  │
                 │                         │ (Java 25, Spring Boot)  │
                 │                         │ jdbc:mysql://mysql:3306 │
                 │                         ▼                         │
  IDE / mvnw ──► │ localhost:3307 ──► container busca-facil-mysql    │
                 │                     (MySQL 8, volume mysql_data)  │
                 └───────────────────────────────────────────────────┘
```

Peças envolvidas:

| Peça | O que é | Para que serve aqui |
|---|---|---|
| **Docker** | Ferramenta que roda aplicações em *containers*: ambientes isolados com seu próprio sistema de arquivos, como uma "mini-máquina" leve. | Rodar o MySQL e o back-end sem instalar nada disso no Windows. |
| **Imagem** | O "molde" de um container: sistema base + programas + arquivos. É gerada a partir de um `Dockerfile`. | `mysql:8.0` (pronta, do Docker Hub) e a imagem do back-end (montada por nós). |
| **Docker Compose** | Ferramenta que sobe vários containers descritos num arquivo YAML (`docker-compose.yml`). | Subir banco + back-end com um único comando, já conectados entre si. |
| **Volume** | Pasta gerenciada pelo Docker que sobrevive quando o container é apagado. | `mysql_data` guarda os dados do banco. |
| **`.env`** | Arquivo de texto com variáveis `NOME=valor`, ignorado pelo Git. | Guardar as senhas fora do código. |

---

## 2. Credenciais: o arquivo `.env`

Na raiz do projeto, `.env` (que **não** vai para o Git, por causa do `.gitignore`):

```
MYSQL_ROOT_PASSWORD=...
DB_USER=...
DB_PASSWORD=...
```

Ele é lido por **dois** consumidores:

1. **Docker Compose:** lê o `.env` da mesma pasta automaticamente. No YAML, `${DB_USER}` vira o valor do arquivo.
2. **Spring Boot (rodando fora do Docker):** lê o `.env` via `spring.config.import` (veja a seção 4).

Por que assim? A senha fica num lugar só, e o código no GitHub não expõe nada. O `.env.example` serve de modelo, com os nomes das variáveis e sem valores, para quem clonar o projeto.

---

## 3. O `docker-compose.yml`

### Serviço `mysql`

- `image: mysql:8.0`: usa a imagem oficial do MySQL.
- `environment`: a imagem oficial lê estas variáveis **na primeira inicialização** para criar o banco `busca_facil`, o usuário e as senhas. Se o volume já existe, elas são ignoradas, porque o banco já foi criado.
- `ports: "3307:3306"`: formato `PORTA_NO_PC:PORTA_NO_CONTAINER`. Usamos 3307 no PC para não conflitar com um MySQL que você já tenha instalado na 3306.
- `volumes: mysql_data:/var/lib/mysql`: `/var/lib/mysql` é onde o MySQL grava os dados. Apontar essa pasta para um volume faz os dados sobreviverem a `docker compose down`.

### Ajuste A: o *healthcheck*

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1"]
  interval: 10s
  timeout: 5s
  retries: 10
```

- **O que é:** um comando que o Docker executa periodicamente para saber se o serviço está *saudável* (*healthy*), e não apenas "ligado".
- **Como funciona:** a cada 10 s, roda `mysqladmin ping`. Se o comando responder, o container fica `healthy`. Depois de 10 falhas, fica `unhealthy`.
- **Por que `127.0.0.1` e não `localhost`:** na primeira inicialização, a imagem do MySQL sobe um servidor *temporário* que só aceita conexões pelo **socket**, um arquivo local, e não pela rede. Com `-h localhost`, o `mysqladmin` usa o socket e responde "ok" cedo demais, quando o servidor definitivo ainda nem subiu. Com `-h 127.0.0.1`, ele usa **TCP**, o protocolo de rede, e só responde quando o servidor de verdade está pronto.

### Serviço `backend`

- `build: ./backend`: em vez de baixar uma imagem pronta, monta a imagem a partir de `backend/Dockerfile`.
- `depends_on: mysql: condition: service_healthy`: o back-end **só inicia depois que o healthcheck do MySQL passa**. Sem isso, o Spring tentaria conectar num banco ainda inicializando e cairia com erro.
- `SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/...`: dentro da rede do Compose, cada serviço é encontrado **pelo nome**. Por isso o host é `mysql`, e não `localhost`. A porta é a **interna**, 3306, e não a 3307 do PC.

---

## 4. Ajuste B: a configuração do Spring (`application.properties`)

Arquivo: `backend/src/main/resources/application.properties`.

### Importando o `.env`

```properties
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
```

- `spring.config.import` diz ao Spring para carregar configurações de outro arquivo.
- `[.properties]` faz o Spring ler o `.env` como se fosse um `.properties`. O formato `NOME=valor` é o mesmo nos dois.
- `optional:` evita erro se o arquivo não existir. É o caso dentro do Docker, onde as variáveis vêm do Compose.
- São dois caminhos porque a aplicação pode ser iniciada da raiz (`.env`) ou de dentro de `backend/` (`../.env`).

### A sintaxe `${VARIAVEL:padrão}`

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3307/busca_facil?...}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:${DB_USER}}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:${DB_PASSWORD}}
```

`${X:Y}` significa: "use X se existir; senão, use Y". Isso dá dois modos de execução:

| Onde roda | `SPRING_DATASOURCE_URL` existe? | URL usada |
|---|---|---|
| Docker (Compose) | Sim, definida no YAML | `jdbc:mysql://mysql:3306/...` |
| Seu PC (IDE / `mvnw`) | Não | `jdbc:mysql://localhost:3307/...`, o MySQL do Docker visto de fora |

Parâmetros da URL JDBC (JDBC é a API padrão do Java para falar com bancos SQL):
- `useSSL=false`: não exige conexão criptografada. Serve para o ambiente local.
- `serverTimezone=UTC`: fixa o fuso horário e evita datas deslocadas.
- `allowPublicKeyRetrieval=true`: necessário para o login com o tipo de senha padrão do MySQL 8 quando o SSL está desligado.

### JPA / Hibernate

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

- **JPA** é a especificação Java para mapear classes em tabelas. O **Hibernate** é a implementação usada pelo Spring.
- `ddl-auto=update`: o Hibernate compara as classes `@Entity` com as tabelas e cria ou altera o que faltar. Isso é prático para estudo. Em produção, o normal é usar uma ferramenta de *migrations*, como o Flyway, que aplica scripts SQL versionados.
- `show-sql=true`: imprime no log todo SQL gerado. É ótimo para aprender o que o Hibernate faz por baixo.

---

## 5. Ajuste C: testes com H2 (sem depender do MySQL)

### O problema

O teste `BackendApplicationTests.contextLoads` usa `@SpringBootTest`, que **sobe a aplicação inteira**, incluindo a conexão com o banco. Sem o MySQL ligado, o teste falhava, mesmo sem nenhum bug no código.

### A solução

**1. Dependência no `pom.xml`:**

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- **H2** é um banco SQL escrito em Java que pode rodar **na memória**: nasce quando o teste começa e some quando termina.
- `scope test`: a biblioteca só existe durante os testes e **não** vai para o `.jar` final.
- Sem `<version>`: o `spring-boot-starter-parent` já define uma versão compatível (*dependency management*).

**2. Arquivo `backend/src/test/resources/application.properties`:**

```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
```

### Por que ele "substitui" o principal

O Java procura arquivos no **classpath**, uma lista ordenada de pastas e `.jar`s. Durante os testes, o Maven coloca `target/test-classes`, vindo de `src/test/resources`, **antes** de `target/classes`, vindo de `src/main/resources`. Os dois arquivos se chamam `application.properties`, então o de teste é encontrado primeiro e o principal **nem é lido**. A consequência é que o `.env` também não é importado nos testes, e eles não precisam de senha nenhuma.

- `MODE=MySQL`: o H2 imita o dialeto do MySQL, para o SQL se comportar de forma parecida.
- `create-drop`: cria as tabelas no início e apaga no fim. Cada execução começa limpa.

### Como rodar

```bash
cd backend
./mvnw test                                              # todos os testes
./mvnw test -Dtest=BackendApplicationTests               # uma classe
./mvnw test -Dtest=BackendApplicationTests#contextLoads  # um método
```

No log, a prova de que usou o H2: `url=jdbc:h2:mem:testdb` e `Database driver: H2 JDBC Driver`.

> Limitação: o H2 em modo MySQL é *parecido*, não idêntico. Para testar recursos específicos do MySQL, no futuro dá para usar o **Testcontainers**, que sobe um MySQL real em Docker durante o teste.

---

## 6. O `backend/Dockerfile` e o Ajuste D

```dockerfile
# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests
# ---------- Etapa 2: executar ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
USER 1000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build *multi-stage*

São duas etapas, cada uma começando com `FROM`:

1. **`build`:** imagem com Maven + JDK (o kit completo de desenvolvimento Java). Compila o código e gera o `.jar`.
2. **Final:** imagem só com o **JRE**, o necessário para *executar* Java. Ela recebe apenas o `.jar` da etapa 1 (`COPY --from=build`).

Por que fazer assim: a imagem final não carrega Maven, código-fonte nem dependências de build. Fica menor e com menos coisas que poderiam ser exploradas.

Outros detalhes:
- `--mount=type=cache,target=/root/.m2`: guarda as dependências baixadas pelo Maven entre builds, o que deixa o segundo build muito mais rápido.
- `-DskipTests`: não roda os testes durante o build da imagem, para o build ser rápido. Os testes rodam com `./mvnw test`.
- `EXPOSE 8080`: documenta a porta usada. Quem de fato publica a porta é o `ports:` do Compose.
- Java 25 aparece no `pom.xml` (`java.version`) **e** nas duas imagens. Se mudar a versão num lugar, mude nos outros.

### Ajuste D: `USER 1000`

- **O problema:** sem `USER`, o processo do container roda como **root** (uid 0), o administrador do Linux. Se alguém explorar uma falha na aplicação, ganha poderes de administrador dentro do container.
- **A solução:** `USER 1000` faz o Java rodar como um usuário comum. A imagem é baseada em Ubuntu e já tem o usuário `ubuntu` com uid 1000.
- **Por que funciona:** a aplicação só precisa *ler* o `app.jar` (permissão `-rw-r--r--`, legível por todos) e abrir a porta 8080. Portas acima de 1024 não exigem root.
- **Por que no final:** `USER` vale para tudo que vem depois dele. As etapas de preparação continuam como root, e só a execução fica sem privilégios.
- **Grupo `sudo`:** o usuário `ubuntu` pertence a esse grupo, mas o programa `sudo` não está instalado na imagem, então o grupo não dá poder extra.

Como conferir:
```bash
docker exec busca-facil-backend id
# uid=1000(ubuntu) gid=1000(ubuntu) ...
```

---

## 7. Comandos do dia a dia

| Situação | Comando |
|---|---|
| Subir tudo (reconstruindo a imagem do back-end) | `docker compose up -d --build` |
| Subir só o banco, para rodar o back-end pela IDE | `docker compose up -d mysql` |
| Ver o estado dos containers | `docker compose ps` |
| Ver os logs do back-end, acompanhando ao vivo | `docker compose logs -f backend` |
| Parar e remover os containers (os dados ficam no volume) | `docker compose down` |
| Parar e **apagar os dados** do banco | `docker compose down -v` ⚠️ |
| Rodar os testes | `cd backend && ./mvnw test` |

- `-d` (*detached*): roda em segundo plano e devolve o terminal.
- `--build`: reconstrói a imagem. Use depois de mudar código Java ou o `Dockerfile`.
- `-v` no `down`: remove os volumes, ou seja, **o banco inteiro**. Só use se quiser começar do zero.

### Preciso parar os containers?

Não é obrigatório. Eles usam juntos cerca de 700 MB de RAM e quase nenhuma CPU quando ociosos.

- Os containers estão com `restart: no`, então **não sobem sozinhos** quando o Docker Desktop ou o PC reinicia.
- Pare (`docker compose down`) quando quiser liberar memória ou quando terminar de estudar.
- `docker compose stop` também funciona: ele **pausa** os containers sem removê-los, e `docker compose start` os retoma.

---

## 8. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Failed to configure a DataSource: 'url' attribute is not specified` | `application.properties` sem a configuração do banco | Conferir a seção 4 |
| `Communications link failure` rodando pela IDE | MySQL do Docker desligado | `docker compose up -d mysql` |
| `Access denied for user` | Senhas do `.env` mudaram depois que o volume foi criado | O MySQL só lê as senhas na 1ª vez: `docker compose down -v` e subir de novo (apaga os dados) |
| `Port 3307 is already allocated` | Outro programa usando a porta | Trocar a porta do lado esquerdo em `ports:` e na URL padrão do `application.properties` |
| Mudança no código não aparece no container | Imagem antiga | `docker compose up -d --build` |
