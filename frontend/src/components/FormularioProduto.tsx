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
