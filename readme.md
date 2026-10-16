# PediuPatrão — Atividade M1

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

## Vídeo
