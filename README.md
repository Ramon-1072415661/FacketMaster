# 🎟️ FacketMaster

> Sistema Distribuído de Venda de Ingressos Online

Projeto desenvolvido para a UC de **Sistemas Distribuídos e Mobile** — UNISUL, 2026.  
Professor: Fernando Costa Leite

**Equipe:** Caio · Kaue · Ramon · Gustavo · Eduardo

---

## Sumário

- [Arquitetura](#arquitetura)
- [Microsserviços](#microsserviços)
- [Infraestrutura AWS](#infraestrutura-aws)
- [Fluxo Principal de Compra](#fluxo-principal-de-compra)
- [Banco de Dados](#banco-de-dados)
- [Autenticação](#autenticação)
- [Tecnologias](#tecnologias)
- [Divisão de Responsabilidades](#divisão-de-responsabilidades)
- [Como Rodar Localmente](#como-rodar-localmente)

---

## Arquitetura

O FacketMaster é construído sobre uma arquitetura de **microsserviços hospedada integralmente na AWS**. Cada domínio funcional é isolado, podendo evoluir, ser implantado e escalar de forma independente.

```
┌─────────────────────────────────────────────┐
│           Camada 1 — Borda                  │
│         AWS API Gateway (JWT, throttling)   │
└─────────────────────┬───────────────────────┘
                      │
┌─────────────────────▼───────────────────────┐
│        Camada 2 — Distribuição              │
│     AWS Application Load Balancer (ALB)     │
└──────┬──────────┬──────────┬────────┬───────┘
       │          │          │        │
┌──────▼──┐ ┌────▼────┐ ┌───▼───┐ ┌──▼──────┐
│  Auth   │ │ Eventos │ │Reserv.│ │Pagament.│
│ Service │ │Service  │ │(Redis)│ │  (stub) │
└──────┬──┘ └────┬────┘ └───┬───┘ └──┬──────┘
       │         │           │        │
┌──────▼─────────▼───────────▼────────▼──────┐
│           Camada 4 — Persistência           │
│   Amazon RDS (PostgreSQL) · Redis · Fila   │
└─────────────────────────────────────────────┘
```

---

## Microsserviços

| Serviço | Responsabilidade |
|---|---|
| **Auth** | Cadastro, login, geração/validação de JWT, controle de acesso por perfil (ADMIN / USER) |
| **Eventos (FacketMaster)** | Ciclo de vida dos eventos: criação, consulta, atualização, controle de estoque, cancelamento via soft delete |
| **Reservas** | Carrinho com reserva temporária usando Redis + TTL; expiração automática libera estoque de carrinhos abandonados |
| **Pagamentos (stub)** | Simula Boleto Bancário, PIX e Cartão de Crédito; publica resultado na fila assíncrona |

---

## Infraestrutura AWS

- **API Gateway** — único ponto de entrada; valida JWT na borda, aplica throttling por rota e centraliza logs
- **Application Load Balancer** — distribui tráfego entre instâncias EC2 stateless, health checks automáticos via `/actuator/health`
- **EC2** — instâncias stateless executando containers Docker de cada microsserviço
- **Amazon RDS (PostgreSQL)** — multi-AZ, failover automático, backups diários gerenciados
- **Redis** — estado efêmero das reservas; operações atômicas (`DECR`/`SETNX`) previnem overselling
- **Fila Assíncrona** — desacopla pagamento, emissão de ingresso e notificação por e-mail; suporte a dead-letter queue

---

## Fluxo Principal de Compra

```
1. POST /auth/login          → token JWT (HMAC256, 24h)
2. GET  /api/v1/eventos      → lista eventos ATIVOS com data futura
3. Inicia reserva            → DECR atômico no Redis com TTL
4. Escolhe forma de pagamento → stub processa e publica na fila
5. Serviço de eventos        → transação ACID: decrementa estoque + cria pedido + emite ingresso
6. Serviço de notificações   → consome evento da fila e simula envio de e-mail (assíncrono)
```

> Se o carrinho não for confirmado dentro do TTL, o estoque é liberado automaticamente.

---

## Banco de Dados

Banco: `venda_ingresso` — PostgreSQL no Amazon RDS  
Migrations gerenciadas pelo **Flyway**, aplicadas automaticamente no startup.

### Schemas e Tabelas

| Schema | Tabelas |
|---|---|
| `auth` | `usuarios` |
| `eventos` | `eventos`, `tipos_ingresso`, `checkin` |
| `vendas` | `pedidos`, `itens_pedido`, `ingressos`, `pagamentos` |

### Constraints de Integridade

```sql
-- tipos_ingresso
CHECK (valor > 0)
CHECK (quant_total > 0)
CHECK (quant_disp >= 0)
CHECK (quant_disp <= quant_total)

-- itens_pedido
CHECK (quantidade > 0)
CHECK (valor_uni > 0)
CHECK (subtotal > 0)

-- pagamentos
CHECK (valor > 0)

-- pedidos
CHECK (valor_total > 0)
```

### Índices

```sql
idx_eventos_status        -- listagem por status (consulta mais frequente)
idx_eventos_data_evento   -- ordenação cronológica
idx_eventos_nome          -- LOWER(nome) para busca case-insensitive
```

### Variáveis de Ambiente

```env
DB_URL=jdbc:postgresql://ingresso-pg.c90e2ss02swi.us-east-1.rds.amazonaws.com:5432/venda_ingresso
DB_USER=postgres
DB_PASSWORD=*********
```

---

## Autenticação

- Tokens **JWT** assinados com **HMAC256**, validade de 24h
- Biblioteca: `auth0/java-jwt 4.4.0`
- Senhas armazenadas como hash **BCrypt** (nunca texto plano)
- Filtro `JwtValidationFilter` intercepta todas as requisições

### Níveis de Acesso

| Rota | Acesso |
|---|---|
| `/auth/register`, `/auth/login` | Público |
| `/auth/admin/**`, `/api/v1/eventos/admin/**` | ADMIN |
| `/api/**` | ADMIN e USER |

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Segurança | Spring Security + JWT |
| Persistência | Spring Data JPA + PostgreSQL |
| Migrations | Flyway |
| Cache / Reservas | Redis |
| Testes | H2 (in-memory) |
| Containers | Docker + docker-compose |
| Logs | Logback + LogstashEncoder (JSON em produção) |
| Métricas | Spring Actuator |
| Nuvem | AWS (API Gateway, ALB, EC2, RDS, Redis) |

---

## Divisão de Responsabilidades

| Membro | Área |
|---|---|
| **Caio** | Infraestrutura AWS — API Gateway, ALB, EC2 |
| **Kaue** | Serviço de Eventos — CRUD, entidades JPA, DTOs, repositório, controller, GlobalExceptionHandler; e este relatório |
| **Ramon** | Serviço de Reservas (Redis + TTL) e Pagamentos (stub + fila assíncrona) |
| **Gustavo** | Banco de Dados — schema relacional, constraints, índices, provisionamento do RDS |
| **Eduardo** | Autenticação JWT + Spring Security; observabilidade (logs estruturados, Actuator) |

---

## Como Rodar Localmente

**Pré-requisitos:** Docker e docker-compose instalados.

```bash
# Clone o repositório
git clone https://github.com/Ramon-1072415661/FacketMaster
cd FacketMaster

# Suba todos os serviços (PostgreSQL, Redis e microsserviços)
docker-compose up --build
```

A API estará disponível em `http://localhost:8080`.

---

*UNISUL — Sistemas Distribuídos e Mobile — 2026*