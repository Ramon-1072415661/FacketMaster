# Banco de Dados

## Tecnologias

* PostgreSQL
* AWS RDS

## Banco

Nome do banco:

```text
venda_ingresso
```

## Estrutura

O banco é composto pelas tabelas responsáveis por:

* Usuários
* Eventos
* Ingressos
* Pedidos
* Pagamentos

## Arquivos

* `schema_venda_ingresso.sql` → Script de criação do banco.
* `DER.png` → Diagrama Entidade-Relacionamento.

## Configuração da aplicação

Variáveis necessárias:

```text
DB_URL=jdbc:postgresql://ingresso-pg.c90e2ss02swi.us-east-1.rds.amazonaws.com:5432/venda_ingresso
DB_USER=postgres
DB_PASSWORD=*********
```
