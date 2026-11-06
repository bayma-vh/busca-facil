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
