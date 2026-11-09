# PediuPatrão

## Como rodar
1. Java 21
2. Variável de ambiente MONGO_URI com a URI do MongoDB
3. ./mvnw.cmd spring-boot:run 
4. abrir http://localhost:8080
5. Testes: ./mvnw.cmd test

## Usuários de teste (senha teste123)
- admin 
- gerente 
- atendente

## Requisitos implementados
- **RF01** — Perfis ADMIN/GERENTE/ATENDENTE; autorização por URL no SecurityConfig e @PreAuthorize em desconto/cancelamento; só ADMIN cria usuários e atribui perfis; senha nunca retorna na API.
- **RF02** — Pedido com itens, quantidades, preço unitário, subtotal, desconto, total, responsável e data/hora de entrada. Preço vem do banco; total calculado no servidor.
- **RF03** — Fluxo RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA/RETIRADO → FINALIZADO validado em StatusPedido; saída registra responsável e data/hora.
- **RF04** — Desconto (0–20%, substitui o anterior) e cancelamento (com justificativa) só para GERENTE e só antes da saída; cancelado não muda mais.
- **RF05** — Coleção auditoria com usuário, perfil, data/hora, operação, entidade, id, campos alterados (anterior/novo) e justificativa. Consulta em /auditoria só para ADMIN e GERENTE. Sem endpoints de alteração/exclusão; gravação via insert. Usuários não são auditados para não registrar senhas.

## Requisitos não funcionais
- **RNF01** — Java 21, Spring Boot 3.4.4, MongoDB (URI via variável de ambiente `MONGO_URI`).
- **RNF02** — Senhas armazenadas com BCrypt; campo `password` marcado `WRITE_ONLY` no Jackson, nunca serializado na resposta.
- **RNF03** — Autenticação via sessão HTTP (form login). CSRF desabilitado. Endpoints protegidos por URL no `SecurityConfig` e por `@PreAuthorize` nos services.
- **RNF04** — Interface web via Thymeleaf + AdminLTE. API REST retorna JSON com status HTTP corretos (200, 201, 204, 400, 403, 404).
- **RNF05** — Regras de negócio (cálculo de total, transições de status, limite de desconto) validadas no servidor.
- **RNF06** — Auditoria gravada via `insert` (imutável). Sem endpoints de alteração ou exclusão do log.
- **RNF07** — Código versionado com GitFlow: `main`, `develop`, `feature/api-endpoints`.

## Endpoints

Base URL: `http://localhost:8080`

---

### Pedidos — `/api/pedidos`

#### `GET /api/pedidos`
Retorna os pedidos ordenados por `dataHoraEntrada`.

Acesso: ADMIN, GERENTE, ATENDENTE

Resposta 200:
```json
[
  {
    "id": "682abc123",
    "clienteId": "abc123",
    "clienteNome": "João Silva",
    "itens": [
      {
        "produtoId": "p01",
        "nomeProduto": "Pizza Calabresa",
        "quantidade": 2,
        "precoUnitario": 45.00,
        "subtotal": 90.00
      }
    ],
    "subtotal": 90.00,
    "percentualDesconto": 0,
    "total": 90.00,
    "status": "RECEBIDO",
    "responsavelEntrada": "atendente",
    "dataHoraEntrada": "2026-11-08T19:30:00"
  }
]
```

---

#### `POST /api/pedidos`
Cria um pedido.

Acesso: GERENTE, ATENDENTE

Body:
```json
{
  "clienteId": "abc123",
  "itens": [
    { "produtoId": "p01", "quantidade": 2 }
  ]
}
```

Resposta 201: pedido criado com `status: "RECEBIDO"`, `responsavelEntrada` = usuário da sessão.

Erros:
- 500 se `clienteId` ou `produtoId` não existir

---

#### `PUT /api/pedidos/{id}/status?status={novoStatus}`
Avança o status do pedido. Transições inválidas lançam `IllegalStateException`.

Acesso: GERENTE, ATENDENTE

Fluxo válido:
```
RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA | RETIRADO → FINALIZADO
```

`SAIU_PARA_ENTREGA` e `RETIRADO` registram `responsavelSaida` e `dataHoraSaida`.

Resposta 200: pedido atualizado.

---

#### `POST /api/pedidos/{id}/desconto`
Aplica desconto. Substitui o desconto anterior e recalcula o total.

Acesso: GERENTE apenas (`@PreAuthorize("hasRole('GERENTE')")` no service)

Body:
```json
{ "percentual": 10 }
```

Restrições: percentual > 0 e ≤ 20; status deve ser RECEBIDO, EM_PREPARACAO ou PRONTO.

Resposta 200: pedido com total recalculado.

Erros:
- 403 se não for GERENTE
- 500 se percentual fora do intervalo ou pedido já saiu

---

#### `POST /api/pedidos/{id}/cancelar`
Cancela o pedido. Registra `canceladoPor`, `dataHoraCancelamento` e `justificativaCancelamento`.

Acesso: GERENTE apenas (`@PreAuthorize("hasRole('GERENTE')")` no service)

Body:
```json
{ "justificativa": "Cliente desistiu" }
```

Restrições: justificativa não pode ser vazia; status deve ser RECEBIDO, EM_PREPARACAO ou PRONTO. Pedido cancelado não aceita mais mudanças de status.

Resposta 200: pedido com `status: "CANCELADO"`.

Erros:
- 403 se não for GERENTE
- 500 se justificativa em branco ou pedido já saiu

---

### Clientes — `/api/clientes`

Acesso para leitura: GERENTE, ATENDENTE. Escrita (POST, PUT): ATENDENTE. DELETE: GERENTE.

#### `GET /api/clientes` → 200: lista de clientes

#### `POST /api/clientes` → 201: cliente criado

Body:
```json
{
  "nome": "Maria Souza",
  "telefone": "11999887766",
  "cep": "01310-100",
  "logradouro": "Av. Paulista",
  "numero": "1000",
  "complemento": "Apto 42",
  "bairro": "Bela Vista",
  "cidade": "São Paulo",
  "estado": "SP"
}
```

#### `PUT /api/clientes/{id}` → 200: cliente atualizado (mesmo body do POST)

#### `DELETE /api/clientes/{id}` → 204

Auditoria registra cada campo alterado individualmente (nome, telefone, cep, logradouro, numero, complemento, bairro, cidade, estado).

---

### Produtos — `/api/produtos`

Acesso para leitura: todos autenticados. Escrita (POST, PUT, DELETE): GERENTE.

#### `GET /api/produtos` → 200: lista de produtos

#### `POST /api/produtos` → 201: produto criado

Body:
```json
{
  "nome": "Pizza Mussarela",
  "tipo": "Pizza",
  "preco": 42.00,
  "ativo": true
}
```

Tipos usados no seed: `Pizza`, `Bebida`. Outros valores são aceitos sem validação.

#### `PUT /api/produtos/{id}` → 200: produto atualizado

#### `DELETE /api/produtos/{id}` → 204

---

### Usuários — `/api/usuarios`

Acesso: ADMIN apenas.

#### `GET /api/usuarios` → 200: lista (sem o campo `password`)

#### `GET /api/usuarios/{id}` → 200 | 404

#### `POST /api/usuarios` → 201

Body:
```json
{
  "username": "novo_atendente",
  "password": "senha123",
  "role": "ATENDENTE"
}
```

Perfis válidos: `ADMIN`, `GERENTE`, `ATENDENTE`. Senha é codificada com BCrypt antes de salvar.

#### `PUT /api/usuarios/{id}` → 200 | 404

Atualiza `username` e `password`. Senha é re-criptografada. O campo `role` não é atualizado por este endpoint (limitação do controller atual).

#### `DELETE /api/usuarios/{id}` → 204 | 404

---

### Auditoria

#### `GET /auditoria`
Tela de auditoria (Thymeleaf, não JSON). Acesso: ADMIN, GERENTE.

Cada registro contém:
```json
{
  "id": "...",
  "usuario": "gerente",
  "perfil": "GERENTE",
  "dataHora": "2026-11-08T19:35:00",
  "operacao": "DESCONTO",
  "entidade": "Pedido",
  "entidadeId": "682abc123",
  "alteracoes": [
    { "campo": "percentualDesconto", "valorAnterior": "0", "valorNovo": "10" },
    { "campo": "total", "valorAnterior": "90.00", "valorNovo": "81.00" }
  ],
  "justificativa": null
}
```

Operações registradas: `CRIACAO_PEDIDO`, `ALTERACAO_STATUS`, `DESCONTO`, `CANCELAMENTO`, `CRIACAO_CLIENTE`, `ALTERACAO_CLIENTE`, `EXCLUSAO_CLIENTE`.

---

## Testes

Testes unitários em `PedidoTest.java`, sem dependência de Spring ou banco:

| Teste | descriçaõ |
|-------|-------------|
| `calculaTotal` | subtotal = preço × quantidade |
| `aplicaDescontoDe10PorCento` | total = subtotal − 10% |
| `bloqueiaDescontoAcimaDe20` | lança `IllegalArgumentException` |
| `bloqueiaTransicaoInvalida` | RECEBIDO → FINALIZADO lança `IllegalStateException` |
| `registraSaidaEBloqueiaDescontoECancelamento` | registra responsável e data na saída; bloqueia desconto e cancelamento após saída |
| `canceladoNaoMudaDeStatus` | CANCELADO → EM_PREPARACAO lança `IllegalStateException` |
| `cancelamentoExigeJustificativa` | justificativa em branco lança `IllegalArgumentException` |

Para rodar: `./mvnw.cmd test`


## Vídeo


https://github.com/user-attachments/assets/94964c17-2f70-4e49-8e24-176f6e8dd03f

