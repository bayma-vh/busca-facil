# Entidade `Produto`: da classe Java à tabela no MySQL

Este documento explica o passo 4.1 do `guia-passo-a-passo.md`: a primeira entidade do Busca Fácil (v0.1, ver `modelo-dados.md`).

Arquivo: `backend/src/main/java/com/busca_facil/backend/produto/Produto.java`

---

## 1. A ideia: ORM

Bancos relacionais guardam **tabelas** (linhas e colunas). Java trabalha com **objetos** (classes e atributos). Traduzir manualmente entre os dois mundos, escrevendo SQL e copiando cada coluna para um atributo, é repetitivo e fácil de errar.

Um **ORM** (*Object-Relational Mapping*, mapeamento objeto-relacional) faz essa tradução automaticamente:

| Mundo Java | Mundo do banco |
|---|---|
| Classe `Produto` | Tabela `produto` |
| Objeto `new Produto()` | Uma linha da tabela |
| Atributo `nome` | Coluna `nome` |
| Atributo `id` com `@Id` | Chave primária |

As peças envolvidas:

- **JPA** (*Jakarta Persistence API*): a **especificação**, um conjunto de regras e anotações (`@Entity`, `@Id`...) no pacote `jakarta.persistence`. Ela diz *o que* fazer.
- **Hibernate**: a **implementação** da JPA usada pelo Spring Boot. É ele quem gera o SQL e conversa com o banco.
- **Spring Data JPA**: a camada do Spring que configura tudo isso automaticamente. É o starter `spring-boot-starter-data-jpa` no `pom.xml`.

Por que separar especificação e implementação? O código usa só anotações da JPA. Se um dia trocássemos o Hibernate por outra implementação, como o EclipseLink, a entidade continuaria igual.

---

## 2. O código, linha a linha

```java
package com.busca_facil.backend.produto;
```
**Pacote** é a "pasta lógica" da classe. Ele precisa ficar **dentro** de `com.busca_facil.backend`, o pacote da `BackendApplication`. Isso será explicado na seção 4.

```java
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
```
O `*` importa todas as anotações da JPA de uma vez (`Entity`, `Table`, `Id`...). As outras três linhas trazem as anotações do Lombok.

```java
@Entity
@Table(name = "produto")
```
- `@Entity`: "esta classe é persistente; guarde seus objetos no banco".
- `@Table(name = "produto")`: o nome da tabela. Sem essa linha, o Hibernate derivaria o nome da classe. Deixar explícito evita surpresas, como diferenças de maiúsculas e minúsculas entre sistemas operacionais.

```java
@Getter @Setter
@NoArgsConstructor
```
Anotações do **Lombok** (seção 3). Geram `getNome()`, `setNome(...)` e assim por diante, além do construtor `Produto()`.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```
- `@Id`: este atributo é a **chave primária**, o identificador único de cada linha.
- `@GeneratedValue(strategy = IDENTITY)`: quem gera o número é o **banco**. No MySQL isso vira `AUTO_INCREMENT`: 1, 2, 3...
- Por que `Long` (com L maiúsculo) e não `long`? `Long` é um objeto e pode ser `null`. Um produto **ainda não salvo** tem `id == null`, e é assim que o Hibernate sabe que precisa fazer `INSERT`, e não `UPDATE`.

```java
@Column(nullable = false, length = 150)
private String nome;
```
- `nullable = false`: gera `NOT NULL`, ou seja, o banco recusa produto sem nome.
- `length = 150`: gera `varchar(150)`. Sem `length`, o padrão da JPA é 255.

```java
@Column(length = 1000)
private String descricao;

@Column(length = 80)
private String categoria;
```
São opcionais (aceitam `NULL`), com tamanhos adequados a cada uso: a descrição é um texto maior, e a categoria um rótulo curto.

> Atributos sem `@Column` também viram colunas. A anotação serve para **ajustar** o mapeamento (tamanho, obrigatoriedade, nome).

---

## 3. Lombok: código gerado na compilação

Sem o Lombok, a classe precisaria de 8 métodos escritos à mão (um get e um set para cada um dos 4 atributos), mais o construtor. O Lombok é um **processador de anotações**: um plugin do compilador `javac` que, ao ver `@Getter`, **escreve esses métodos no `.class`** durante a compilação. O código-fonte fica curto, e o compilado fica completo.

No `pom.xml`, ele aparece em dois lugares:
- como dependência, com `<optional>true</optional>`, porque só é necessário para compilar;
- em `annotationProcessorPaths` do `maven-compiler-plugin`, que é o que ativa o processador.

Prova: o comando `javap -p`, que mostra o conteúdo de um `.class`, aplicado ao `Produto.class` compilado:

```
public java.lang.Long getId();
public java.lang.String getNome();
...
public void setNome(java.lang.String);
...
public com.busca_facil.backend.produto.Produto();
```

Os métodos existem no compilado, embora não estejam no `.java`.

**Por que a JPA exige o construtor sem argumentos?** Quando o Hibernate lê uma linha do banco, ele precisa criar um objeto vazio (`new Produto()`) e depois preencher os campos. Sem esse construtor, ele não consegue instanciar a classe.

> Por que não usar o `@Data` do Lombok? Ele também gera `equals`, `hashCode` e `toString` usando todos os campos. Em entidades JPA isso causa problemas: o `hashCode` muda quando o `id` é gerado, e com relacionamentos (v0.2) o `toString` pode entrar em loop. Por isso usamos só `@Getter` e `@Setter`.

---

## 4. Como o Spring encontra a entidade

A `BackendApplication` tem `@SpringBootApplication`. Entre outras coisas, essa anotação faz o Spring **varrer o próprio pacote e todos os subpacotes** à procura de componentes e entidades.

```
com.busca_facil.backend              ← BackendApplication (ponto de partida da varredura)
└── com.busca_facil.backend.produto  ← Produto (encontrado)
```

Se `Produto` estivesse num pacote fora dessa árvore, como `com.outro.produto`, ele seria ignorado e a tabela não seria criada.

### Organização por funcionalidade

O pacote se chama `produto/`, e não `entity/`. É a organização **por funcionalidade** (*package by feature*): tudo relacionado a Produto fica junto.

```
produto/
├── Produto.java             ← entidade (feito)
├── ProdutoRepository.java   ← acesso ao banco (próximo passo)
├── ProdutoService.java      ← regras de negócio
└── ProdutoController.java   ← endpoints HTTP
```

Quando chegarem Loja e Oferta (v0.2), cada uma terá seu pacote. A alternativa, *package by layer* (`entity/`, `repository/`, `controller/`), espalha uma mesma funcionalidade por várias pastas.

---

## 5. Da classe à tabela: o que acontece ao iniciar

1. O Spring Boot inicia e configura o Hibernate.
2. O Hibernate encontra as classes `@Entity` (seção 4).
3. Por causa de `spring.jpa.hibernate.ddl-auto=update`, ele compara as entidades com as tabelas existentes. A tabela `produto` não existe, então ele a cria.
4. Por causa de `spring.jpa.show-sql=true`, o SQL aparece no log.

**DDL** (*Data Definition Language*) é a parte do SQL que define estruturas: `CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`.

### O SQL gerado no MySQL (log do container)

```sql
create table produto (
  id bigint not null auto_increment,
  categoria varchar(80),
  descricao varchar(1000),
  nome varchar(150) not null,
  primary key (id)
) engine=InnoDB
```

### O SQL gerado no H2 (testes)

```sql
create table produto (
  id bigint generated by default as identity,
  categoria varchar(80),
  nome varchar(150) not null,
  descricao varchar(1000),
  primary key (id)
)
```

A **mesma classe** gerou SQL diferente para cada banco. Isso é o **dialeto** do Hibernate: ele sabe que o MySQL usa `auto_increment` e o H2 usa `identity`. `engine=InnoDB` é o mecanismo de armazenamento padrão do MySQL, que suporta transações e chaves estrangeiras.

### Tradução de tipos

| Java | MySQL |
|---|---|
| `Long` | `bigint` (inteiro de 64 bits) |
| `String` + `length = 150` | `varchar(150)` |
| `nullable = false` | `not null` |
| `@Id` | `primary key` |
| `GenerationType.IDENTITY` | `auto_increment` |

---

## 6. Como verificar

**Pelos testes (H2, sem Docker):**
```bash
cd backend
./mvnw test -Dspring.jpa.show-sql=true
```
O `-Dspring.jpa.show-sql=true` liga o log de SQL só nesta execução. O arquivo de propriedades dos testes não liga essa opção. Procure no log por `create table produto`.

**No MySQL real (Docker):**
```bash
docker compose up -d --build              # reconstrói a imagem com a classe nova
docker compose logs backend | grep -i "create table"
```

**Olhando a tabela dentro do MySQL:**
```bash
docker exec -it busca-facil-mysql mysql -u <seu DB_USER> -p busca_facil
```
- `docker exec -it`: executa um comando dentro de um container que já está rodando. `-it` deixa o comando interativo, para você digitar.
- `mysql -u ... -p`: o cliente de linha de comando do MySQL. O `-p` sozinho faz ele **pedir** a senha, sem deixá-la no histórico do terminal.

Depois, dentro do cliente:
```sql
DESCRIBE produto;
```
Resultado:
```
Field      Type           Null  Key  Extra
id         bigint         NO    PRI  auto_increment
categoria  varchar(80)    YES
descricao  varchar(1000)  YES
nome       varchar(150)   NO
```

---

## 7. Cuidados com `ddl-auto=update`

- **Ele só adiciona, nunca remove.** Se você renomear `descricao` para `detalhes`, o Hibernate cria a coluna `detalhes` e **mantém** `descricao` lá, órfã.
- **Ele não altera tamanho nem tipo de colunas existentes** de forma confiável. Mudar `length = 150` para `200` geralmente não muda a tabela.
- Para recomeçar do zero no ambiente de estudo: `docker compose down -v` (⚠️ apaga **todos** os dados) e subir de novo.

É por isso que, em projetos reais, se usa uma ferramenta de **migrations**, como o Flyway. Cada mudança no banco vira um script SQL numerado (`V1__cria_produto.sql`, `V2__...`), versionado no Git e aplicado em ordem.

---

## 8. Próximo passo

Uma entidade sozinha só cria a tabela. Para **salvar e buscar** produtos, o próximo passo é o `ProdutoRepository`: uma interface que estende `JpaRepository` e ganha automaticamente métodos como `save`, `findAll`, `findById` e `deleteById`, sem escrever SQL.
