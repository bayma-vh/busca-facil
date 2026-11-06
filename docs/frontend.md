# Frontend do Busca Fácil: guia passo a passo

Este guia mostra como construir, do zero, a interface web do Busca Fácil. Ele é escrito para quem **acabou de chegar ao projeto**: não supõe que você conheça o histórico, nem React. Cada ferramenta, termo e comando é explicado na primeira vez que aparece.

Ao final você terá uma página em `http://localhost:5173` que **busca, lista, cadastra, edita e remove produtos**, conversando com a API que já existe no repositório.

![Tela final do frontend](tela-final.png)

> Testado em 05/11/2026 com Node.js 22, Vite 8, React 19 e TypeScript 7, contra o backend da `main` (commit `0e1bd87`).

## Sumário

0. [Contexto: o que já existe e o que vamos construir](#0-contexto-o-que-já-existe-e-o-que-vamos-construir)
1. [Pré-requisitos](#1-pré-requisitos)
2. [Clonar o repositório e criar a branch](#2-clonar-o-repositório-e-criar-a-branch)
3. [Subir o backend](#3-subir-o-backend)
4. [Criar o projeto do frontend com o Vite](#4-criar-o-projeto-do-frontend-com-o-vite)
5. [Conceitos de React que vamos usar](#5-conceitos-de-react-que-vamos-usar)
6. [Configurar o proxy (e entender o CORS)](#6-configurar-o-proxy-e-entender-o-cors)
7. [A camada de API: `src/api.ts`](#7-a-camada-de-api-srcapits)
8. [Componente 1: `BarraDeBusca`](#8-componente-1-barradebusca)
9. [Componente 2: `CartaoProduto`](#9-componente-2-cartaoproduto)
10. [Componente 3: `FormularioProduto`](#10-componente-3-formularioproduto)
11. [O componente principal: `App.tsx`](#11-o-componente-principal-apptsx)
12. [Estilos: `index.css`](#12-estilos-indexcss)
13. [Rodar e testar na prática](#13-rodar-e-testar-na-prática)
14. [Verificações antes de commitar](#14-verificações-antes-de-commitar)
15. [Commit, push e Pull Request](#15-commit-push-e-pull-request)
16. [Problemas comuns](#16-problemas-comuns)
17. [Próximos passos](#17-próximos-passos)

---

## 0. Contexto: o que já existe e o que vamos construir

O **Busca Fácil** é um projeto de estudo: uma plataforma para buscar produtos e comparar preços entre lojas. Ele é feito em **versões**:

| Versão | O que entra | Estado |
|---|---|---|
| v0.1 | Produto (cadastro e busca) | **Backend pronto** (está na `main`) |
| v0.2 | Loja e Oferta (preços, comparação, histórico) | Ainda não feito |
| v0.3 | Usuário (login) | Ainda não feito |
| v0.4 | Favoritos | Ainda não feito |

O repositório tem três pastas principais:

- `backend/`: a **API REST** em Java 25 + Spring Boot. Uma API REST é um programa que responde a requisições HTTP (como as que o navegador faz) devolvendo dados em **JSON**, um formato de texto para representar objetos (`{"nome": "Notebook"}`).
- `frontend/`: **vazia**. É aqui que vamos trabalhar.
- `docs/`: documentação. O arquivo `docs/v0.1.md` descreve a API em detalhes.

### A API que vamos consumir

Tudo que o frontend faz é chamar estas rotas do backend:

| O que faz | Método e rota | Resposta de sucesso |
|---|---|---|
| Listar todos | `GET /produtos` | `200` + lista de produtos |
| Buscar por nome | `GET /produtos?nome=note` | `200` + lista filtrada (pedaço do nome, sem diferenciar maiúsculas) |
| Cadastrar | `POST /produtos` com JSON no corpo | `201` + produto criado |
| Alterar | `PUT /produtos/{id}` com JSON no corpo | `200` + produto alterado |
| Remover | `DELETE /produtos/{id}` | `204` (sem corpo) |

**Método HTTP** é o "verbo" da requisição: `GET` lê, `POST` cria, `PUT` substitui, `DELETE` apaga. O **status** é o número que diz se deu certo (`2xx`) ou se houve erro do cliente (`4xx`).

Um produto no JSON tem este formato:

```json
{ "id": 1, "nome": "Notebook Dell", "descricao": "16 GB RAM", "categoria": "Informática" }
```

Quando algo dá errado (ex.: nome vazio), o backend responde `400` com um JSON no formato **Problem Details** (um padrão, a RFC 9457):

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos são inválidos.",
  "erros": { "nome": "é obrigatório" }
}
```

Vamos usar o campo `erros` para mostrar a mensagem embaixo do campo certo no formulário.

### Por que esta stack

| Ferramenta | O que é | Por que aqui |
|---|---|---|
| **Node.js** | Programa que executa JavaScript fora do navegador. | Todas as ferramentas de frontend rodam em cima dele. |
| **npm** | Gerenciador de pacotes que vem com o Node. Baixa bibliotecas (listadas no `package.json`) para a pasta `node_modules/` e roda scripts. | É o "Maven do frontend": `package.json` ≈ `pom.xml`. |
| **React** | Biblioteca para montar interfaces a partir de **componentes** (funções que devolvem pedaços de tela). Quando os dados mudam, o React redesenha só o que mudou. | A biblioteca de interface mais usada; o plano futuro do projeto (Next.js) também é React. |
| **TypeScript** | JavaScript com **tipos**, como no Java. Um compilador confere os tipos e gera JavaScript comum. | Erros como "esse campo não existe" aparecem no editor, antes de rodar. |
| **Vite** | Ferramenta de desenvolvimento e build: serve os arquivos na porta 5173, recarrega a página quando você salva, e gera os arquivos finais otimizados. | Simples e rápido. Tem um **proxy** que evita mexer no backend por causa de CORS (seção 6). |
| **oxlint** | **Linter**: um programa que lê o código procurando erros comuns e más práticas, sem executá-lo. | Já vem no modelo do Vite. |
| **`fetch`** | Função nativa do navegador para fazer requisições HTTP. | Não precisa instalar biblioteca extra. |
| **CSS puro** | A linguagem de estilos do navegador. | Suficiente para um MVP; evita mais uma ferramenta. |

**Por que não Next.js agora?** O README cita Next.js como plano. O Next.js é um framework **em cima** do React que acrescenta renderização no servidor e rotas por pastas. Para uma demonstração que só conversa com a API Spring, isso traz muitos conceitos sem necessidade. Os componentes React deste guia continuam servindo se o projeto migrar depois.

**Por que só Produto?** Preços e histórico dependem das tabelas `Loja` e `Oferta`, que só existem na v0.2. Não vamos inventar preços falsos no frontend, porque o formato inventado provavelmente não bateria com o que a API vai devolver.

---

## 1. Pré-requisitos

Os comandos deste guia são para o **PowerShell** no Windows (o terminal padrão do Windows). No macOS ou Linux, os comandos funcionam iguais no terminal, exceto onde indicado.

### 1.1 Git

**Git** é o sistema de controle de versões: guarda o histórico de cada mudança no código e permite trabalhar em paralelo com **branches** (linhas de desenvolvimento separadas).

```powershell
git --version
```

Se aparecer uma versão (`git version 2.x`), está instalado. Se não, baixe em <https://git-scm.com>.

### 1.2 Docker Desktop

**Docker** roda programas dentro de **containers**: ambientes isolados que já vêm com tudo o que o programa precisa. Com ele você sobe o banco MySQL e o backend Java **sem instalar Java nem MySQL** na sua máquina. O **Docker Desktop** é o aplicativo que roda o Docker no Windows/macOS.

Instale em <https://www.docker.com/products/docker-desktop>, **abra o Docker Desktop** (ele precisa estar rodando) e confira:

```powershell
docker --version
docker compose version
```

### 1.3 Node.js (versão LTS)

Baixe a versão **LTS** em <https://nodejs.org>. LTS (*Long Term Support*) é a versão estável com suporte longo; evite a "Current". Instale com as opções padrão, **feche e abra o PowerShell** (para ele enxergar o programa novo) e confira:

```powershell
node --version
npm --version
```

Algo como `v22.x.x` (ou mais novo) e `10.x.x`. O Vite 8 precisa do Node 20.19 ou mais novo.

### 1.4 Editor

Recomendado: **Visual Studio Code** (<https://code.visualstudio.com>). Ele entende TypeScript sem configuração e mostra os erros de tipo enquanto você digita.

---

## 2. Clonar o repositório e criar a branch

Escolha uma pasta para seus projetos (ex.: `C:\projetos`), abra o PowerShell nela e rode:

```powershell
git clone https://github.com/bayma-vh/busca-facil.git
cd busca-facil
```

- `git clone <url>`: baixa o repositório inteiro (arquivos e histórico) do GitHub para uma pasta nova, `busca-facil`. O GitHub fica registrado com o apelido **`origin`**.
- `cd busca-facil`: entra na pasta (*change directory*).

Se você **já tinha** o repositório clonado, em vez disso atualize a `main`:

```powershell
git checkout main
git pull origin main
```

- `git checkout main`: muda para a branch `main`, trocando os arquivos da pasta pelos dela.
- `git pull origin main`: baixa do GitHub os commits novos da `main` e os aplica na sua cópia.

Agora crie a sua branch de trabalho **a partir da `main`** (é nela que está o backend da v0.1):

```powershell
git checkout -b feature/v0.1-frontend
```

- `git checkout -b <nome>`: cria (`-b` = *branch*) uma branch nova a partir de onde você está e já muda para ela. O prefixo `feature/` é a convenção do projeto para branches de funcionalidade.

**Como conferir:**

```powershell
git branch
```

Lista as branches locais; a atual tem um `*` na frente: `* feature/v0.1-frontend`.

---

## 3. Subir o backend

O frontend sem backend não tem dados para mostrar. Vamos subir o MySQL e a API com o Docker.

### 3.1 Criar o arquivo `.env`

O `docker-compose.yml` lê senhas de um arquivo `.env` na raiz. Esse arquivo **não vai para o Git** (está no `.gitignore`, a lista de arquivos que o Git ignora), porque contém senhas. Existe um modelo, o `.env.example`. Na **raiz do repositório**:

```powershell
Copy-Item .env.example .env
notepad .env
```

- `Copy-Item origem destino`: copia um arquivo (no macOS/Linux: `cp .env.example .env`).
- `notepad .env`: abre o arquivo no Bloco de Notas.

Preencha com valores à sua escolha (é só para a sua máquina) e salve:

```
MYSQL_ROOT_PASSWORD=root123
DB_USER=busca
DB_PASSWORD=busca123
```

### 3.2 Subir os containers

Com o Docker Desktop aberto, na **raiz do repositório**:

```powershell
docker compose up -d --build
```

- `docker compose`: lê o `docker-compose.yml`, que descreve os containers do projeto (o MySQL e o backend).
- `up`: cria e liga os containers.
- `-d` (*detached*): roda em segundo plano e devolve o terminal para você.
- `--build`: compila a imagem do backend a partir do `backend/Dockerfile` (uma "receita" que compila o Java e prepara o container).

A primeira vez demora alguns minutos (baixa as imagens e compila). **Confira:**

```powershell
docker compose ps
```

Os dois containers (`busca-facil-mysql` e `busca-facil-backend`) devem aparecer com status `running` (o MySQL também mostra `healthy`). Depois abra <http://localhost:8080/produtos> no navegador. Deve aparecer `[]` (lista vazia) ou uma lista de produtos em JSON.

Se a página não abrir, espere uns 20 segundos (o backend espera o MySQL ficar pronto) e veja os logs com `docker compose logs backend`.

---

## 4. Criar o projeto do frontend com o Vite

### 4.1 Gerar o projeto

Na **raiz do repositório**:

```powershell
npm create vite@latest frontend -- --template react-ts
```

- `npm create vite@latest`: baixa e roda o criador de projetos do Vite (o pacote `create-vite`), na versão mais recente.
- `frontend`: a pasta onde o projeto será criado. Ela já existe e está vazia, então o criador pode perguntar o que fazer com ela; escolha a opção de **ignorar os arquivos existentes** (ou de continuar).
- `--`: separa as opções do npm das opções do Vite.
- `--template react-ts`: usa o modelo React + TypeScript.

Se ele perguntar **"Install with npm and start now?"**, responda **No**. Vamos fazer esses passos à mão, para entender cada um.

### 4.2 Instalar as dependências

```powershell
cd frontend
npm install
```

- `npm install`: lê o `package.json` e baixa as bibliotecas para `node_modules/`. Também cria o `package-lock.json`, que registra a **versão exata** de cada biblioteca instalada, para que todos no time instalem exatamente o mesmo.

### 4.3 Ver o modelo funcionando

```powershell
npm run dev
```

- `npm run dev`: executa o script `dev` definido no `package.json`, que liga o servidor de desenvolvimento do Vite.

O terminal mostra `Local: http://localhost:5173/`. Abra no navegador: aparece a página de exemplo do Vite. Aperte `Ctrl+C` no terminal para parar.

### 4.4 O que foi criado

| Arquivo | Para que serve |
|---|---|
| `package.json` | Nome do projeto, dependências e scripts (`dev`, `build`, `lint`, `preview`) |
| `package-lock.json` | Versões exatas instaladas (vai para o Git) |
| `node_modules/` | As bibliotecas baixadas (**não** vai para o Git; o `.gitignore` do Vite já a ignora) |
| `index.html` | A **única** página HTML. O React monta a aplicação dentro da `<div id="root">` |
| `src/main.tsx` | Ponto de entrada: liga o React à `<div id="root">` e desenha o componente `App` |
| `src/App.tsx` | Componente principal (vamos reescrever) |
| `src/App.css`, `src/index.css` | Estilos do exemplo (vamos substituir) |
| `src/assets/`, `public/` | Imagens do exemplo |
| `vite.config.ts` | Configuração do Vite |
| `tsconfig*.json` | Configuração do TypeScript (como ele confere os tipos) |
| `.oxlintrc.json` | Configuração do linter |
| `.gitignore` | O que o Git deve ignorar dentro de `frontend/` |

`.tsx` é TypeScript com **JSX**: uma sintaxe que permite escrever algo parecido com HTML dentro do código (`<h1>Olá</h1>`). O Vite traduz isso para chamadas JavaScript comuns.

### 4.5 Limpar o exemplo

Apague o que é só do exemplo (dentro de `frontend/`):

```powershell
Remove-Item src/App.css, public/icons.svg
Remove-Item -Recurse src/assets
```

- `Remove-Item`: apaga arquivos (no macOS/Linux: `rm src/App.css public/icons.svg` e `rm -r src/assets`).
- `-Recurse`: apaga a pasta com tudo dentro.

Mantenha `public/favicon.svg` (o ícone da aba). Os arquivos `src/App.tsx` e `src/index.css` serão reescritos nas próximas seções.

Em `index.html`, troque o idioma e o título da aba:

```html
<html lang="pt-BR">
...
    <title>Busca Fácil</title>
```

`lang="pt-BR"` diz ao navegador (e a leitores de tela) que o texto está em português.

### 4.6 A estrutura final

Ao terminar o guia, `frontend/src/` terá:

```
src/
├── main.tsx                     (já veio pronto, não mexemos)
├── index.css                    estilos da aplicação inteira
├── api.ts                       tudo que fala com o backend
├── App.tsx                      componente principal: guarda o estado e junta as peças
└── components/
    ├── BarraDeBusca.tsx         caixa de busca
    ├── CartaoProduto.tsx        um produto da lista
    └── FormularioProduto.tsx    cadastro e edição
```

Crie a pasta dos componentes:

```powershell
New-Item -ItemType Directory src/components
```

(no macOS/Linux: `mkdir src/components`)

A ideia dessa divisão é a mesma das **camadas** do backend: `api.ts` faz o papel de "cliente HTTP" (como um repository), os componentes só cuidam da tela, e o `App` coordena.

---

## 5. Conceitos de React que vamos usar

Leia esta seção uma vez; o código das próximas seções usa todos esses conceitos e os comentários fazem referência a eles.

**Componente.** Uma função com nome em maiúscula que devolve JSX. Usamos como se fosse uma tag: `<CartaoProduto />`.

```tsx
function Saudacao() {
  return <h1>Olá!</h1>
}
```

**Props.** Os "parâmetros" de um componente, passados como atributos: `<Saudacao nome="Ana" />`. O componente recebe um objeto `{ nome }`. Props são **somente leitura**: quem manda nelas é o componente pai. Para o filho avisar o pai de algo, o pai passa uma **função** como prop (ex.: `aoBuscar`), e o filho a chama.

**Estado (`useState`).** Um valor que o componente lembra entre um desenho e outro. Quando ele muda, o React redesenha o componente.

```tsx
const [contador, setContador] = useState(0)   // valor inicial 0
// contador: o valor atual. setContador(5): troca o valor e redesenha.
```

Nunca altere o estado diretamente (`contador = 5` não funciona); sempre use a função `set...`.

**Efeito (`useEffect`).** Código que roda **depois** que o componente aparece na tela, para falar com o "mundo de fora" (aqui, buscar dados na API). O segundo argumento é a lista de dependências: `[]` significa "rode só uma vez, quando o componente aparecer".

**Input controlado.** O `value` do `<input>` vem do estado e o `onChange` atualiza o estado a cada tecla. Assim o React sempre sabe o que está digitado:

```tsx
<input value={termo} onChange={(e) => setTermo(e.target.value)} />
```

**Renderização condicional.** `{condicao && <p>...</p>}` só desenha o `<p>` se a condição for verdadeira. `{a ? <X /> : <Y />}` escolhe entre dois.

**Listas e `key`.** `lista.map(item => <li key={item.id}>...</li>)` transforma cada item num elemento. A `key` identifica cada item, para o React saber qual mudou, entrou ou saiu. Use algo único e estável, como o `id`.

**`async` / `await`.** Uma requisição HTTP demora. Uma função `async` pode usar `await` para **esperar** um resultado sem travar a tela. Ela devolve uma `Promise` ("um valor que vai chegar depois"). `try { ... } catch { ... }` captura erros, como no Java.

---

## 6. Configurar o proxy (e entender o CORS)

### O problema

O frontend vai rodar em `http://localhost:5173` e o backend em `http://localhost:8080`. Para o navegador, endereços com portas diferentes são **origens diferentes**. Por segurança, o navegador bloqueia que uma página leia respostas de outra origem, a menos que o servidor autorize explicitamente. Essa regra se chama **CORS** (*Cross-Origin Resource Sharing*). Sem tratar isso, todo `fetch` falha com um erro de CORS no console.

### A solução para desenvolvimento: proxy

Um **proxy** é um intermediário que recebe uma requisição e a repassa para outro lugar. O Vite tem um embutido: o navegador pede `http://localhost:5173/produtos`, e o Vite repassa para `http://localhost:8080/produtos` e devolve a resposta. Para o navegador, tudo veio da mesma origem (`5173`), então não há CORS, e **o backend não precisa mudar**.

```
Navegador ──► Vite (5173) ──► /produtos? ──► Spring (8080) ──► MySQL
          ◄──────────────────────────────────────────────────
```

(Em produção, quando frontend e backend estiverem publicados, será preciso configurar o CORS no Spring ou servir os dois no mesmo endereço. Isso fica para depois.)

Substitua todo o conteúdo de `frontend/vite.config.ts`:

```ts
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Toda requisição que começa com /produtos é repassada ao backend Spring.
      // Assim o navegador só conversa com o Vite (mesma origem) e não há bloqueio de CORS.
      '/produtos': 'http://localhost:8080',
    },
  },
})
```

- `defineConfig`: função do Vite que só serve para o editor saber o formato da configuração (autocompletar e checagem de tipos).
- `plugins: [react()]`: ativa o suporte a React (JSX e recarregamento rápido).
- `server.proxy`: cada chave é um prefixo de caminho; o valor é para onde repassar. Quando a v0.2 trouxer `/lojas` e `/ofertas`, basta acrescentar linhas.

---

## 7. A camada de API: `src/api.ts`

Este arquivo concentra **toda** a comunicação com o backend. Os componentes só chamam `listarProdutos()`, `criarProduto(...)` etc. Se uma URL mudar, mexemos num lugar só.

Crie `frontend/src/api.ts`:

```ts
// Tudo que conversa com o backend fica neste arquivo.
// Os componentes só chamam estas funções e não precisam saber de URLs nem de HTTP.

// Formato do JSON que o backend DEVOLVE (igual ao ProdutoResponse do Java).
export type Produto = {
  id: number
  nome: string
  descricao: string | null
  categoria: string | null
}

// Formato do JSON que o backend RECEBE (igual ao ProdutoRequest do Java).
// Não tem id: quem gera o id é o banco.
export type DadosProduto = {
  nome: string
  descricao: string
  categoria: string
}

// Erro da API no formato Problem Details (RFC 9457), como o TratadorDeErros devolve.
export class ErroApi extends Error {
  status: number
  erros: Record<string, string>

  constructor(status: number, mensagem: string, erros: Record<string, string> = {}) {
    super(mensagem)
    this.status = status
    this.erros = erros
  }
}

// Função central: faz a requisição e transforma respostas de erro em ErroApi.
async function requisitar<T>(url: string, opcoes: RequestInit = {}): Promise<T> {
  const resposta = await fetch(url, {
    ...opcoes,
    headers: { 'Content-Type': 'application/json', ...opcoes.headers },
  })

  if (!resposta.ok) {
    // Tenta ler o corpo do erro; se não for JSON (ex.: backend fora do ar), usa uma mensagem genérica.
    const problema = await resposta.json().catch(() => null)
    throw new ErroApi(
      resposta.status,
      problema?.detail ?? `Erro ${resposta.status} ao falar com o servidor.`,
      problema?.erros ?? {},
    )
  }

  // 204 No Content (remoção) não tem corpo para ler.
  if (resposta.status === 204) return undefined as T
  return resposta.json()
}

export function listarProdutos(nome = ''): Promise<Produto[]> {
  const filtro = nome.trim()
  const url = filtro ? `/produtos?nome=${encodeURIComponent(filtro)}` : '/produtos'
  return requisitar<Produto[]>(url)
}

export function criarProduto(dados: DadosProduto): Promise<Produto> {
  return requisitar<Produto>('/produtos', { method: 'POST', body: JSON.stringify(dados) })
}

export function atualizarProduto(id: number, dados: DadosProduto): Promise<Produto> {
  return requisitar<Produto>(`/produtos/${id}`, { method: 'PUT', body: JSON.stringify(dados) })
}

export function removerProduto(id: number): Promise<void> {
  return requisitar<void>(`/produtos/${id}`, { method: 'DELETE' })
}
```

O que cada parte faz:

- **`export type Produto`**: define o formato de um objeto, como um `record` do Java. Espelha o `ProdutoResponse` do backend. `string | null` significa "texto ou nulo" (descrição e categoria são opcionais e o backend devolve `null` quando estão vazias). `export` deixa outros arquivos importarem.
- **`DadosProduto`**: o que enviamos ao criar ou alterar (espelha o `ProdutoRequest`). Não tem `id`, porque quem gera o id é o banco. Aqui os campos são sempre texto (vazio = `''`); o backend transforma texto em branco em `null`.
- **`class ErroApi extends Error`**: um tipo de erro nosso, que carrega o `status` HTTP e o mapa `erros` (campo → mensagem) vindo do Problem Details. `extends Error` é herança, como no Java. `Record<string, string>` é um objeto com chaves e valores de texto, como um `Map<String, String>`.
- **`requisitar<T>(...)`**: a função central. O `<T>` é um **genérico** (como `List<T>` no Java): quem chama diz qual tipo espera receber (`requisitar<Produto[]>`).
  - `fetch(url, opcoes)` faz a requisição. `RequestInit` é o tipo das opções do `fetch` (método, cabeçalhos, corpo).
  - `...opcoes` (**spread**) copia todas as propriedades de `opcoes` para o novo objeto; depois sobrescrevemos `headers` para sempre incluir `Content-Type: application/json`, que avisa ao Spring que o corpo é JSON.
  - `resposta.ok` é `true` para status `200` a `299`. Se não for, lemos o corpo do erro e lançamos um `ErroApi`.
  - `.catch(() => null)`: se o corpo do erro não for JSON (ex.: o proxy não alcançou o backend), em vez de quebrar, usamos `null`.
  - `problema?.detail` (**optional chaining**): se `problema` for `null`, não dá erro; devolve `undefined`. `?? 'texto'` (**nullish coalescing**) usa o texto da direita quando a esquerda for `null` ou `undefined`.
  - O status `204` (remoção) não tem corpo; tentar ler JSON dele daria erro, por isso o tratamento separado.
- **`listarProdutos`**: `nome = ''` é um **valor padrão** do parâmetro. `trim()` tira os espaços das pontas. `encodeURIComponent` converte espaços e acentos para um formato válido na URL (`café` → `caf%C3%A9`). A crase (`` `...${x}...` ``) é uma **template string**: texto com valores embutidos.
- **`JSON.stringify(dados)`**: transforma o objeto em texto JSON para enviar no corpo.

---

## 8. Componente 1: `BarraDeBusca`

A caixa de busca. Ela guarda o que está sendo digitado e, quando o formulário é enviado (botão ou Enter), avisa o pai pela prop `aoBuscar`.

Crie `frontend/src/components/BarraDeBusca.tsx`:

```tsx
import { useState, type FormEvent } from 'react'

type Props = {
  aoBuscar: (termo: string) => void
}

// Caixa de texto + botão. Quando o formulário é enviado, avisa o componente pai com o termo digitado.
export default function BarraDeBusca({ aoBuscar }: Props) {
  const [termo, setTermo] = useState('')

  function enviar(evento: FormEvent) {
    evento.preventDefault() // impede o navegador de recarregar a página
    aoBuscar(termo)
  }

  function limpar() {
    setTermo('')
    aoBuscar('')
  }

  return (
    <form className="busca" onSubmit={enviar}>
      <input
        type="search"
        placeholder="Buscar produto pelo nome..."
        value={termo}
        onChange={(e) => setTermo(e.target.value)}
      />
      <button type="submit">Buscar</button>
      {termo && (
        <button type="button" className="secundario" onClick={limpar}>
          Limpar
        </button>
      )}
    </form>
  )
}
```

- **`type Props`**: o formato das props. `aoBuscar: (termo: string) => void` é "uma função que recebe um texto e não devolve nada".
- **`{ aoBuscar }: Props`**: **desestruturação**, que tira a propriedade `aoBuscar` do objeto de props direto para uma variável.
- **`import { ..., type FormEvent }`**: o `type` indica que estamos importando só um tipo (não existe no JavaScript final).
- **`<form onSubmit={enviar}>`**: usar um `<form>` faz o Enter funcionar de graça. Por padrão, enviar um formulário **recarrega a página**; `evento.preventDefault()` cancela isso, porque quem cuida de tudo é o React.
- **`type="search"`**: um campo de texto próprio para buscas (no celular, o teclado mostra "Buscar").
- **`type="button"`** no "Limpar": dentro de um `<form>`, todo botão é de envio por padrão; `type="button"` faz ele só executar o `onClick`.
- **`{termo && (...)}`**: o botão "Limpar" só aparece quando há algo digitado.

---

## 9. Componente 2: `CartaoProduto`

Mostra um produto. É um componente **"burro"** (só exibe): não sabe falar com a API, só avisa o pai quando um botão é clicado.

Crie `frontend/src/components/CartaoProduto.tsx`:

```tsx
import type { Produto } from '../api'

type Props = {
  produto: Produto
  aoEditar: (produto: Produto) => void
  aoRemover: (produto: Produto) => void
}

// Mostra um produto. Não sabe falar com a API: só avisa o pai quando um botão é clicado.
export default function CartaoProduto({ produto, aoEditar, aoRemover }: Props) {
  return (
    <li className="cartao">
      <div>
        <h3>{produto.nome}</h3>
        {produto.categoria && <span className="etiqueta">{produto.categoria}</span>}
        {produto.descricao && <p>{produto.descricao}</p>}
      </div>
      <div className="acoes">
        <button type="button" className="secundario" onClick={() => aoEditar(produto)}>
          Editar
        </button>
        <button type="button" className="perigo" onClick={() => aoRemover(produto)}>
          Remover
        </button>
      </div>
    </li>
  )
}
```

- **`import type { Produto } from '../api'`**: importa o tipo do arquivo `api.ts`, que está uma pasta acima (`..`).
- **`className`**: em JSX, o atributo `class` do HTML se escreve `className`, porque `class` é palavra reservada do JavaScript.
- **`onClick={() => aoEditar(produto)}`**: passamos uma **arrow function** (função curta, `() => ...`). Se escrevêssemos `onClick={aoEditar(produto)}`, a função seria chamada **na hora de desenhar**, e não no clique.
- **`<li>`**: cada cartão é um item da lista `<ul>` que o `App` monta.

---

## 10. Componente 3: `FormularioProduto`

O mesmo formulário serve para **cadastrar** e **editar**. A diferença é a prop `produtoEmEdicao`: `null` significa cadastro; um produto significa edição. Ele também mostra os erros de validação que o backend devolve.

Crie `frontend/src/components/FormularioProduto.tsx`:

```tsx
import { useState, type FormEvent } from 'react'
import { ErroApi, type DadosProduto, type Produto } from '../api'

type Props = {
  produtoEmEdicao: Produto | null // null = cadastrando um novo
  aoSalvar: (dados: DadosProduto) => Promise<void>
  aoCancelar: () => void
}

const VAZIO: DadosProduto = { nome: '', descricao: '', categoria: '' }

// Converte um Produto (que pode ter null) nos dados do formulário (sempre texto).
function dadosIniciais(produto: Produto | null): DadosProduto {
  if (!produto) return VAZIO
  return {
    nome: produto.nome,
    descricao: produto.descricao ?? '',
    categoria: produto.categoria ?? '',
  }
}

// Serve para CADASTRAR e para EDITAR: muda só o título e o que acontece ao salvar.
// O App passa uma "key" diferente para cada produto; quando a key muda, o React cria o
// formulário do zero, e o useState abaixo começa de novo com os dados do produto escolhido.
export default function FormularioProduto({ produtoEmEdicao, aoSalvar, aoCancelar }: Props) {
  const [dados, setDados] = useState<DadosProduto>(dadosIniciais(produtoEmEdicao))
  const [errosDosCampos, setErrosDosCampos] = useState<Record<string, string>>({})
  const [mensagem, setMensagem] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  // Atualiza um campo só, mantendo os outros (o "..." copia o objeto antigo).
  function alterar(campo: keyof DadosProduto, valor: string) {
    setDados({ ...dados, [campo]: valor })
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setSalvando(true)
    setErrosDosCampos({})
    setMensagem(null)
    try {
      await aoSalvar(dados)
      if (!produtoEmEdicao) setDados(VAZIO) // depois de cadastrar, limpa para o próximo
    } catch (e) {
      if (e instanceof ErroApi) {
        setMensagem(e.message)
        setErrosDosCampos(e.erros) // ex.: { nome: "é obrigatório" }
      } else {
        setMensagem('Não foi possível falar com o servidor.')
      }
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form className="formulario" onSubmit={enviar}>
      <h2>{produtoEmEdicao ? `Editando: ${produtoEmEdicao.nome}` : 'Cadastrar produto'}</h2>

      <label>
        Nome *
        <input value={dados.nome} onChange={(e) => alterar('nome', e.target.value)} />
        {errosDosCampos.nome && <small className="erro">{errosDosCampos.nome}</small>}
      </label>

      <label>
        Categoria
        <input value={dados.categoria} onChange={(e) => alterar('categoria', e.target.value)} />
        {errosDosCampos.categoria && <small className="erro">{errosDosCampos.categoria}</small>}
      </label>

      <label>
        Descrição
        <textarea
          rows={3}
          value={dados.descricao}
          onChange={(e) => alterar('descricao', e.target.value)}
        />
        {errosDosCampos.descricao && <small className="erro">{errosDosCampos.descricao}</small>}
      </label>

      {mensagem && <p className="erro">{mensagem}</p>}

      <div className="acoes">
        <button type="submit" disabled={salvando}>
          {salvando ? 'Salvando...' : produtoEmEdicao ? 'Salvar alterações' : 'Cadastrar'}
        </button>
        {produtoEmEdicao && (
          <button type="button" className="secundario" onClick={aoCancelar}>
            Cancelar
          </button>
        )}
      </div>
    </form>
  )
}
```

- **`aoSalvar: (dados) => Promise<void>`**: a prop é uma função **assíncrona**. O formulário espera (`await`) o pai salvar para saber se deu certo ou se precisa mostrar erros.
- **`dadosIniciais(...)`**: converte o `Produto` (que pode ter `null`) em texto para os inputs. `produto.descricao ?? ''` usa `''` quando a descrição é `null`, porque um input não aceita `null` como valor.
- **`useState(dadosIniciais(produtoEmEdicao))`**: o estado começa com os dados do produto. Repare que **não** há `useEffect` para "recarregar o formulário quando o produto muda". Em vez disso, o `App` passa uma **`key`** diferente para cada produto (seção 11). Quando a `key` de um componente muda, o React o descarta e cria outro do zero, e o `useState` começa de novo. É o jeito recomendado pelo React para "reiniciar" um componente.
- **Quatro estados**: `dados` (o que está nos inputs), `errosDosCampos` (mensagens por campo vindas do backend), `mensagem` (erro geral) e `salvando` (para desabilitar o botão e evitar clique duplo).
- **`alterar(campo, valor)`**: `keyof DadosProduto` é o tipo `'nome' | 'descricao' | 'categoria'`, então o TypeScript recusa um nome de campo errado. `{ ...dados, [campo]: valor }` cria um objeto novo copiando os campos antigos e trocando só um. `[campo]` entre colchetes é uma **chave calculada**: usa o valor da variável como nome da propriedade. Criamos um objeto **novo** em vez de alterar o antigo porque o React só percebe a mudança quando recebe um objeto diferente.
- **`try / catch / finally`**: `finally` roda sempre, dando certo ou não; ali voltamos `salvando` para `false`.
- **`e instanceof ErroApi`**: verifica se o erro é o nosso (tem `erros` por campo) ou outro qualquer (ex.: rede fora).
- **`<label>` envolvendo o `<input>`**: liga o texto ao campo; clicar no texto foca o campo, e leitores de tela anunciam o nome certo.
- **`disabled={salvando}`**: enquanto salva, o botão fica desabilitado.

---

## 11. O componente principal: `App.tsx`

O `App` é o "maestro": guarda o estado que vários componentes compartilham (a lista, o termo buscado, qual produto está em edição) e chama a API.

Por que o estado fica aqui e não nos filhos? Porque a lista precisa ser recarregada quando o **formulário** salva e quando o **cartão** remove. O estado fica no **ancestral comum** de quem o usa, e desce para os filhos por props. Isso se chama **"elevar o estado"** (*lifting state up*).

Substitua todo o conteúdo de `frontend/src/App.tsx`:

```tsx
import { useEffect, useState } from 'react'
import {
  atualizarProduto,
  criarProduto,
  listarProdutos,
  removerProduto,
  type DadosProduto,
  type Produto,
} from './api'
import BarraDeBusca from './components/BarraDeBusca'
import CartaoProduto from './components/CartaoProduto'
import FormularioProduto from './components/FormularioProduto'

const MENSAGEM_SEM_BACKEND =
  'Não foi possível carregar os produtos. O backend está rodando na porta 8080?'

// Componente principal: guarda o estado da tela e coordena os componentes filhos.
export default function App() {
  const [produtos, setProdutos] = useState<Produto[]>([])
  const [termo, setTermo] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [emEdicao, setEmEdicao] = useState<Produto | null>(null)

  async function carregar(nome: string) {
    setCarregando(true)
    setErro(null)
    try {
      setProdutos(await listarProdutos(nome))
    } catch {
      setErro(MENSAGEM_SEM_BACKEND)
    } finally {
      setCarregando(false)
    }
  }

  // Executa uma vez, quando a tela abre ([] = sem dependências): carrega todos os produtos.
  // O "carregando" já começa como true, então aqui só esperamos a resposta.
  useEffect(() => {
    listarProdutos()
      .then(setProdutos)
      .catch(() => setErro(MENSAGEM_SEM_BACKEND))
      .finally(() => setCarregando(false))
  }, [])

  function buscar(novoTermo: string) {
    setTermo(novoTermo)
    carregar(novoTermo)
  }

  async function salvar(dados: DadosProduto) {
    if (emEdicao) {
      await atualizarProduto(emEdicao.id, dados)
      setEmEdicao(null)
    } else {
      await criarProduto(dados)
    }
    await carregar(termo) // recarrega a lista para mostrar a mudança
  }

  async function remover(produto: Produto) {
    if (!confirm(`Remover "${produto.nome}"?`)) return
    try {
      await removerProduto(produto.id)
      if (emEdicao?.id === produto.id) setEmEdicao(null)
      await carregar(termo)
    } catch {
      setErro(`Não foi possível remover "${produto.nome}".`)
    }
  }

  return (
    <main>
      <header>
        <h1>Busca Fácil</h1>
        <p>Pesquise e cadastre produtos. Preços e histórico chegam na v0.2.</p>
      </header>

      <BarraDeBusca aoBuscar={buscar} />

      {erro && <p className="erro">{erro}</p>}

      <section>
        <h2>{termo ? `Resultados para "${termo}"` : 'Todos os produtos'}</h2>
        {carregando ? (
          <p>Carregando...</p>
        ) : produtos.length === 0 ? (
          <p className="vazio">Nenhum produto encontrado.</p>
        ) : (
          <ul className="lista">
            {produtos.map((p) => (
              <CartaoProduto key={p.id} produto={p} aoEditar={setEmEdicao} aoRemover={remover} />
            ))}
          </ul>
        )}
      </section>

      <FormularioProduto
        key={emEdicao?.id ?? 'novo'}
        produtoEmEdicao={emEdicao}
        aoSalvar={salvar}
        aoCancelar={() => setEmEdicao(null)}
      />
    </main>
  )
}
```

- **`carregando` começa como `true`**: a tela abre mostrando "Carregando..." até a primeira resposta chegar.
- **`carregar(nome)`**: busca a lista com o filtro e atualiza o estado. É usada depois de buscar, salvar e remover.
- **`useEffect(..., [])`**: roda uma vez, quando a tela abre. Usa `.then/.catch/.finally`, que é outra forma de lidar com `Promise` (equivalente a `await` + `try/catch/finally`). `.then(setProdutos)` passa a resposta direto para `setProdutos`.
  - Por que não chamar `carregar('')` aqui? Porque `carregar` faz `setCarregando(true)` logo de cara, e mudar o estado de forma **síncrona** dentro de um efeito faz o React desenhar a tela duas vezes à toa. O linter (seção 14) avisa sobre isso.
- **`salvar(dados)`**: decide entre `atualizarProduto` e `criarProduto` conforme haja um produto em edição. Se a API der erro, a exceção "sobe" para o `FormularioProduto`, que mostra as mensagens.
- **`remover(produto)`**: `confirm(...)` abre a caixa nativa do navegador "OK/Cancelar" e devolve `true` ou `false`. `emEdicao?.id`: se nada estiver em edição, não dá erro.
- **`aoEditar={setEmEdicao}`**: passamos a própria função `set` como prop; quando o cartão chama `aoEditar(produto)`, o produto entra em edição.
- **`key={emEdicao?.id ?? 'novo'}`**: a chave que reinicia o formulário (explicada na seção 10). Editar o produto 3 dá `key=3`; cancelar volta para `key='novo'`, e o formulário aparece limpo.
- **Ternário encadeado** na lista: se está carregando, mostra "Carregando..."; senão, se a lista está vazia, "Nenhum produto encontrado."; senão, a lista.

O `src/main.tsx` não muda: ele já importa o `App` e o `index.css`.

---

## 12. Estilos: `index.css`

Substitua todo o conteúdo de `frontend/src/index.css`:

```css
/* Variáveis de cor: mudar aqui muda a aplicação inteira. */
:root {
  --fundo: #f5f6f8;
  --cartao: #ffffff;
  --texto: #1f2328;
  --suave: #656d76;
  --borda: #d0d7de;
  --primaria: #0b6bcb;
  --perigo: #c62828;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  color: var(--texto);
  background: var(--fundo);
}

* { box-sizing: border-box; }
body { margin: 0; }

main { max-width: 760px; margin: 0 auto; padding: 2rem 1rem; }
header p { color: var(--suave); margin-top: 0; }
h1 { margin-bottom: .25rem; }
h2 { font-size: 1.15rem; }
h3 { margin: 0 0 .25rem; font-size: 1rem; }

input, textarea, button { font: inherit; }
input, textarea {
  width: 100%; padding: .55rem .7rem;
  border: 1px solid var(--borda); border-radius: 6px; background: #fff;
}

button {
  padding: .55rem 1rem; border: 1px solid var(--primaria); border-radius: 6px;
  background: var(--primaria); color: #fff; cursor: pointer; white-space: nowrap;
}
button:disabled { opacity: .6; cursor: wait; }
button.secundario { background: #fff; color: var(--primaria); }
button.perigo { background: #fff; color: var(--perigo); border-color: var(--perigo); }

.busca { display: flex; gap: .5rem; }

.lista { list-style: none; padding: 0; margin: 0; display: grid; gap: .6rem; }
.cartao {
  display: flex; justify-content: space-between; gap: 1rem; align-items: flex-start;
  background: var(--cartao); border: 1px solid var(--borda); border-radius: 8px; padding: .9rem 1rem;
}
.cartao p { margin: .4rem 0 0; color: var(--suave); }
.etiqueta { font-size: .78rem; background: #e7f0fb; color: var(--primaria); padding: .1rem .5rem; border-radius: 999px; }

.acoes { display: flex; gap: .5rem; }

.formulario {
  margin-top: 2rem; display: grid; gap: .8rem;
  background: var(--cartao); border: 1px solid var(--borda); border-radius: 8px; padding: 1rem;
}
.formulario h2 { margin: 0; }
.formulario label { display: grid; gap: .3rem; font-weight: 600; font-size: .9rem; }

.erro { color: var(--perigo); font-weight: 400; }
.vazio { color: var(--suave); }

/* Em telas estreitas (celular), os botões do cartão vão para baixo do texto. */
@media (max-width: 560px) {
  .cartao { flex-direction: column; }
}
```

- **`:root { --fundo: ... }`**: **variáveis CSS** (*custom properties*). Definimos as cores uma vez e usamos com `var(--fundo)`. Para mudar o tema, muda-se só aqui.
- **`box-sizing: border-box`**: faz a largura de um elemento incluir a borda e o espaçamento interno, o que evita campos "estourando" a largura.
- **`display: flex`**: coloca os filhos lado a lado (barra de busca, botões). **`display: grid`** com `gap`: empilha os filhos com espaço uniforme entre eles (lista, formulário).
- **`@media (max-width: 560px)`**: regras que só valem em telas estreitas, como celulares. Ali os botões do cartão vão para baixo do texto. Isso é **design responsivo**.

---

## 13. Rodar e testar na prática

Com o backend no ar (seção 3), dentro de `frontend/`:

```powershell
npm run dev
```

Abra <http://localhost:5173> e siga este roteiro:

| # | Faça | Deve acontecer |
|---|---|---|
| 1 | Abra a página | "Todos os produtos" e "Nenhum produto encontrado." (se o banco estiver vazio) |
| 2 | Clique em **Cadastrar** com o nome vazio | Aparece "é obrigatório" embaixo do Nome e "Um ou mais campos são inválidos." (resposta `400` do backend) |
| 3 | Cadastre "Notebook Dell", categoria "Informática", descrição "16 GB RAM" | O produto aparece na lista e o formulário fica limpo |
| 4 | Cadastre "Mouse Logitech" e "Notebook Lenovo" | Três produtos na lista |
| 5 | Busque `note` | Só os dois notebooks; o título vira `Resultados para "note"` |
| 6 | Clique em **Editar** no Notebook Dell | O formulário mostra "Editando: Notebook Dell", com os campos preenchidos |
| 7 | Mude o nome para "Notebook Dell XPS" e salve | O nome muda na lista e o formulário volta para "Cadastrar produto" |
| 8 | Clique em **Remover** no Lenovo e confirme | Ele some da lista |
| 9 | Clique em **Limpar** | Volta a lista completa |
| 10 | Pare o backend (`docker compose stop backend`) e recarregue a página | Aparece "Não foi possível carregar os produtos. O backend está rodando na porta 8080?" |

Para religar o backend: `docker compose start backend`.

**Ferramenta essencial: DevTools.** Aperte `F12` no navegador. Na aba **Network** (Rede) você vê cada requisição que a página fez: método, URL, status e a resposta. Na aba **Console** aparecem os erros de JavaScript. É por ali que se descobre a maior parte dos problemas.

Você também pode conferir que os dados foram mesmo para o banco, abrindo <http://localhost:8080/produtos> direto.

**Recarregamento automático:** com o `npm run dev` rodando, mude um texto no `App.tsx` e salve. A página atualiza sozinha, sem perder o que está digitado. Isso é o **HMR** (*Hot Module Replacement*) do Vite.

---

## 14. Verificações antes de commitar

Dentro de `frontend/`, pare o `npm run dev` (`Ctrl+C`) e rode:

```powershell
npm run lint
npm run build
```

- **`npm run lint`**: roda o **oxlint**. Quando está tudo certo, ele não imprime nada além do cabeçalho `> oxlint`. Se houver problema, ele mostra arquivo, linha e uma explicação do aviso.
- **`npm run build`**: faz duas coisas. Primeiro, `tsc -b` (o compilador do TypeScript) **confere todos os tipos**; um campo com nome errado ou uma prop faltando aparece aqui como erro. Depois, `vite build` gera a versão final e otimizada em `frontend/dist/`. A saída esperada termina com `✓ built in ...ms`.

A pasta `dist/` é gerada; **não** vai para o Git (já está no `.gitignore` do Vite).

Opcional: `npm run preview` serve a versão de `dist/` em <http://localhost:4173>, para ver como fica a versão final. (O proxy não vale no `preview`, então a lista vai dar erro; é esperado.)

Por fim, troque o conteúdo do `frontend/README.md` (o README de exemplo do Vite) por algo útil para o time:

````markdown
# Frontend do Busca Fácil

Interface web (React + TypeScript + Vite) para o backend em `../backend`.

```bash
npm install     # baixa as dependências (só na primeira vez ou quando o package.json mudar)
npm run dev     # sobe em http://localhost:5173 (o backend precisa estar em http://localhost:8080)
npm run build   # verifica os tipos e gera a versão final em dist/
```

O guia completo está em `../docs/frontend.md`.
````

E salve este guia no repositório como `docs/frontend.md` (e a imagem como `docs/tela-final.png`), para o README do frontend apontar para ele.

---

## 15. Commit, push e Pull Request

Volte para a **raiz do repositório** (`cd ..`).

### 15.1 Conferir o que mudou

```powershell
git status
```

Mostra os arquivos novos e alterados. Confira que **não** aparecem `frontend/node_modules/`, `frontend/dist/` nem `.env`. Se aparecer algum deles, **pare**: o `.gitignore` não está funcionando, e esses arquivos não podem ir para o repositório (`node_modules` tem dezenas de milhares de arquivos; o `.env` tem senhas).

Devem aparecer: `frontend/` (pasta nova inteira) e, se você salvou o guia, `docs/frontend.md` e `docs/tela-final.png`.

### 15.2 Commitar

```powershell
git add frontend docs/frontend.md docs/tela-final.png
git commit -m "feat: adiciona frontend MVP com busca e CRUD de produtos"
```

- `git add <caminhos>`: coloca os arquivos na **área de preparação** (*staging*), ou seja, marca o que vai entrar no próximo commit.
- `git commit -m "..."`: grava um **commit** (uma "foto" das mudanças preparadas) com a mensagem. O projeto usa **Conventional Commits**: um prefixo que diz o tipo da mudança (`feat:` = funcionalidade nova, `docs:` = documentação, `chore:` = manutenção), mensagem em português.

O `package-lock.json` **deve** ir no commit (está dentro de `frontend/`).

### 15.3 Enviar para o GitHub

```powershell
git push -u origin feature/v0.1-frontend
```

- `git push`: envia os commits da sua branch para o GitHub.
- `-u` (*upstream*): liga a sua branch local à branch remota de mesmo nome. Nos próximos pushes basta `git push`.

### 15.4 Abrir o Pull Request

Um **Pull Request (PR)** é um pedido para juntar (*merge*) a sua branch em outra, com espaço para revisão e comentários antes.

1. Abra <https://github.com/bayma-vh/busca-facil>. O GitHub mostra um aviso amarelo com **"Compare & pull request"** para a branch recém-enviada; clique nele.
2. Em **base**, escolha a branch que o time usa como destino (a v0.1 do backend foi para a `main`). Em **compare**, a `feature/v0.1-frontend`.
3. Título sugerido: `feat: frontend MVP de produtos (v0.1)`. Na descrição, conte o que foi feito, como testar (o roteiro da seção 13) e, se quiser, cole uma captura de tela.
4. Clique em **Create pull request** e peça revisão.

---

## 16. Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `node` ou `npm` "não é reconhecido como comando" | PowerShell aberto antes da instalação | Feche e abra o PowerShell; se persistir, reinstale o Node marcando "Add to PATH" |
| `npm run dev` diz `Missing script: "dev"` | Comando rodado fora de `frontend/` | `cd frontend` e rode de novo |
| Página mostra "O backend está rodando na porta 8080?" | Backend parado ou ainda subindo | `docker compose ps`; se parado, `docker compose up -d`; veja `docker compose logs backend` |
| No terminal do Vite: `http proxy error: /produtos ... ECONNREFUSED` | Mesmo caso acima: o Vite não alcançou o `8080` | Idem |
| Erro de **CORS** no console | O código chamou `http://localhost:8080/...` direto | Use só caminhos relativos (`/produtos`); quem repassa é o proxy |
| `404` em `/produtos` pelo `5173` | O proxy não foi configurado, ou o Vite não foi reiniciado depois de mudar o `vite.config.ts` | Confira a seção 6 e reinicie o `npm run dev` |
| `docker compose up` falha com erro sobre o `.env` ou senha | `.env` não criado ou vazio | Seção 3.1 |
| Backend sobe, mas falha com "Access denied for user" | Você mudou o `.env` depois de o MySQL já ter criado o banco | `docker compose down -v` (**apaga os dados do banco**) e `docker compose up -d --build` |
| Porta `5173` em uso | Outro `npm run dev` aberto | Feche o outro terminal, ou use a porta que o Vite sugerir |
| `npm run build` com erro de tipo | Diferença entre o código e o tipo (ex.: nome de campo errado) | A mensagem diz arquivo, linha e o que esperava; o VS Code sublinha o mesmo erro em vermelho |
| `git status` mostra `node_modules` | `.gitignore` do frontend apagado | Restaure o `frontend/.gitignore` gerado pelo Vite |

---

## 17. Próximos passos

| Quando | O que entra no frontend |
|---|---|
| Opcional, já | Página de detalhe com **React Router** (biblioteca de navegação entre "páginas" sem recarregar) e testes de componentes com **Vitest** + **Testing Library** |
| v0.2 (Loja e Oferta) | Tabela comparando o preço do produto em cada loja, destaque do menor preço e **gráfico do histórico** (ex.: biblioteca **Recharts**) usando `preco` + `data_coleta` das ofertas; novas rotas no proxy |
| v0.3 (Usuário) | Tela de login; aí será preciso configurar **CORS e autenticação no Spring** |
| v0.4 (Favoritos) | Botão de favoritar |
| Futuro | Colocar o frontend no `docker-compose.yml` e, se o time quiser, migrar para Next.js |
