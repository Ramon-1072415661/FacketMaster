# FacketMaster

Sistema distribuído de venda de ingressos, composto por dois serviços independentes: gerenciamento de eventos e processamento de pagamentos.

---

## Arquitetura

```
Cliente (Bruno)
      │
      ▼
   Nginx (porta 80) ─── load balancer / API gateway
      │
      ├─── event-service  × 2 instâncias  (porta 8081)
      │         │
      │         └─ PostgreSQL · Redis · RabbitMQ
      │
      └─── payment-service × 2 instâncias (porta 8082)
                │
                └─ PostgreSQL · Redis · RabbitMQ
```

A comunicação entre serviços é feita exclusivamente via **RabbitMQ**, nunca por chamada HTTP direta entre eles (exceto a reserva de ingressos, que é síncrona por ser crítica para consistência).

---

## Como rodar

### Pré-requisitos

- Docker e Docker Compose
- Arquivo `.env` na raiz (use `.env.example` como base)

```bash
cp .env.example .env
# preencha os valores no .env
```

### Subindo tudo

```bash
docker compose up -d
```

### Serviços disponíveis

| Serviço              | URL                      | Credenciais            |
|----------------------|--------------------------|------------------------|
| API (nginx)          | http://localhost         | —                      |
| event-service        | http://localhost:8081    | —                      |
| payment-service      | http://localhost:8082    | —                      |
| RabbitMQ Management  | http://localhost:15672   | ver `.env`             |
| Grafana              | http://localhost:3000    | admin / admin          |
| Prometheus           | http://localhost:9090    | —                      |

### Parando

```bash
docker compose down          # preserva volumes
docker compose down -v       # remove banco e cache também
```

---

## Coleção Bruno

Importe a pasta `bruno-collections/` no Bruno e selecione o ambiente **global**.

O ambiente já possui variáveis como `user_token`, `admin_token`, `event_id` e `order_id`. Os scripts de resposta preenchem essas variáveis automaticamente — basta executar os requests na ordem certa.

---

## Fluxo de criação de eventos

```
1. Register Admin  →  POST /auth/admin/register
2. Login Admin     →  POST /auth/login            → salva admin_token
3. Create Event    →  POST /api/v1/eventos/admin/create  → salva event_id
```

### Endpoints de gestão (requerem ADMIN)

| Ação                  | Método | Rota                                          |
|-----------------------|--------|-----------------------------------------------|
| Criar evento          | POST   | `/api/v1/eventos/admin/create`                |
| Atualizar evento      | PATCH  | `/api/v1/eventos/admin/{id}`                  |
| Atualizar preço       | PATCH  | `/api/v1/eventos/admin/preco/{id}`            |
| Atualizar quantidade  | PATCH  | `/api/v1/eventos/admin/quantidade/{id}`       |
| Atualizar status      | PATCH  | `/api/v1/eventos/admin/status/{id}`           |
| Deletar evento        | DELETE | `/api/v1/eventos/admin/{id}`                  |

### Endpoints públicos (requerem USER ou ADMIN)

| Ação              | Método | Rota                        |
|-------------------|--------|-----------------------------|
| Listar eventos    | GET    | `/api/v1/eventos`           |
| Buscar por ID     | GET    | `/api/v1/eventos/{id}`      |

---

## Fluxo de criação de pedidos

```
1. Register User  →  POST /auth/register
2. Login User     →  POST /auth/login       → salva user_token
3. Create Order   →  POST /api/v1/pedidos   → salva order_id
         │
         │  (síncrono, antes de criar o pedido)
         ├─ event-service.reservar()  →  decrementa quantidade_disponivel
         │    └─ falha com 409 se não houver estoque
         │
         │  (assíncrono, após commit da transação)
         └─ RabbitMQ: PedidoPagamentoMessage
                  │
                  ▼
         PaymentConsumer
                  │
                  ├─ chama gateway mock (CARTÃO / PIX / BOLETO)
                  ├─ atualiza status do pedido no banco
                  └─ publica ResultadoPagamentoMessage
                           │
                           ▼
                  event-service consumer
                           │
                           ├─ APROVADO  → confirma reserva (ingressos emitidos)
                           └─ RECUSADO  → libera quantidade de volta ao estoque
```

A criação de pedido retorna **HTTP 202** imediatamente. O processamento pelo gateway ocorre de forma assíncrona pela fila.

---

## Estados do pedido

```
                     ┌─────────────────────┐
                     │  AGUARDANDO_PAGAMENTO │  ← criado pelo POST /pedidos
                     └──────────┬──────────┘
                                │ consumer processa
                                ▼
                         ┌────────────┐
                         │ PROCESSANDO │
                         └─────┬──────┘
               ┌───────────────┼───────────────┐
               │               │               │
         (CARTÃO aprovado)  (PIX / BOLETO)  (qualquer recusado)
               │               │               │
               ▼               ▼               ▼
          ┌─────────┐   ┌─────────────┐  ┌──────────┐
          │ APROVADO │   │ PROCESSANDO │  │ RECUSADO │
          └─────────┘   │ (aguardando │  └──────────┘
                        │  webhook)   │
                        └──────┬──────┘
                               │ POST /pedidos/{id}/confirmar
                               ▼
                          ┌─────────┐
                          │ APROVADO │
                          └─────────┘
```

---

## Condições dos gateways mock

### CARTÃO DE CRÉDITO

Processamento **síncrono** — o resultado final (APROVADO ou RECUSADO) sai da fila na mesma passagem.

| Resultado | Condição                                  | Motivo               |
|-----------|-------------------------------------------|----------------------|
| APROVADO  | número não termina em `0000` e total ≤ R$ 5.000 | —               |
| RECUSADO  | últimos 4 dígitos do cartão = `0000`      | `CARTAO_INVALIDO`    |
| RECUSADO  | valor total > R$ 5.000                    | `LIMITE_INSUFICIENTE` |
| RECUSADO  | `dadosCartao` ausente no body             | `DADOS_CARTAO_AUSENTES` |

**Exemplo para forçar recusa:**
```json
"numero": "1234.5678.9012.0000"
```

---

### PIX

Processamento **assíncrono** — o gateway gera a chave/QR Code e aguarda confirmação externa.

| Resultado   | Condição                  | Motivo                      |
|-------------|---------------------------|-----------------------------|
| PROCESSANDO | valor total ≤ R$ 10.000   | chave PIX gerada, aguardando pagamento |
| RECUSADO    | valor total > R$ 10.000   | `VALOR_ACIMA_DO_LIMITE_PIX` |

Para aprovar um pedido PIX após a geração da chave:
```
POST /api/v1/pedidos/{order_id}/confirmar
```

---

### BOLETO

Processamento **assíncrono** — o gateway gera o código de barras e aguarda compensação bancária.

| Resultado   | Condição                | Motivo               |
|-------------|-------------------------|----------------------|
| PROCESSANDO | valor total ≥ R$ 5,00   | boleto gerado, aguardando pagamento |
| RECUSADO    | valor total < R$ 5,00   | `VALOR_MINIMO_BOLETO` |

Para aprovar um pedido BOLETO após a geração:
```
POST /api/v1/pedidos/{order_id}/confirmar
```

---

## Endpoints de pedidos

| Ação                        | Método | Rota                              | Auth  |
|-----------------------------|--------|-----------------------------------|-------|
| Criar pedido                | POST   | `/api/v1/pedidos`                 | USER  |
| Buscar pedido por ID        | GET    | `/api/v1/pedidos/{id}`            | USER  |
| Listar pedidos do usuário   | GET    | `/api/v1/pedidos`                 | USER  |
| Confirmar pagamento (webhook simulado) | POST | `/api/v1/pedidos/{id}/confirmar` | ADMIN |
