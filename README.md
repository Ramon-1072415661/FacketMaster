# FacketMaster

Sistema distribuído de venda de ingressos, composto por dois serviços independentes: gerenciamento de eventos e
processamento de pagamentos.

---

## Como rodar

### Pré-requisitos

- Docker e Docker Compose instalados

### Subindo tudo

Na raiz do projeto, onde está o `docker-compose.yml`:

```bash
docker compose up -d
```

Isso sobe o banco de dados, o Redis, o RabbitMQ e os dois serviços automaticamente.

### Serviços disponíveis após subir

| Serviço             | URL                    |
|---------------------|------------------------|
| event-service       | http://localhost:8081  |
| payment-service     | http://localhost:8082  |
| RabbitMQ Management | http://localhost:15672 |

Credenciais do RabbitMQ: `guest` / `guest`

### Parando

```bash
docker compose down
```

Para remover também os volumes (banco e cache):

```bash
docker compose down -v
```
