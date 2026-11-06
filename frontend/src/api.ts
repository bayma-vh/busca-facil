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
