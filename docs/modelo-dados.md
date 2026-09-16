## Modelo de dados

- **Produto** (id, nome, descrição, categoria) - V0.1

- **Loja** (id, nome, site) - v0.2

- **Oferta** (id, produto_id, loja_id, preço,data_coleta) - v0.2

- **Usuario** (id, nome, email, senha_hash) - v0.3

- **Favorito** (usuario_id, produto_id) - v0.4

## Relacionamentos

-Um produto tem várias ofertas (1:N)
-uma loja tem várias ofertas (1:N)
-Um Usuário tem vários Favoritos, que apontam para Produtos (N:N via tabela Favorito)