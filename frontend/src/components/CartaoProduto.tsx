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
