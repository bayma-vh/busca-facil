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
