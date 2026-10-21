# Evidência — infraestrutura local (perfil A)

Data: 2026-10-21 · Ambiente: Docker na máquina pessoal (Windows) · Comando: `docker compose up -d --wait`

## Containers saudáveis

```
NAME                         IMAGE                STATUS
selecao-interna-kafka-1      apache/kafka:4.1.1   Up (healthy)
selecao-interna-postgres-1   postgres:17          Up (healthy)
selecao-interna-redis-1      redis:7.4-alpine     Up (healthy)
```

## Bases segregadas (critério 1)

Cada serviço tem banco e usuário próprios, criados por `infra/postgres/01-create-databases.sh`.

Usuário `vagas_svc` no seu próprio banco, acesso permitido:

```
docker compose exec postgres psql -U vagas_svc -d vagas -c "select 1"
 ?column?
----------
        1
```

Usuário `vagas_svc` no banco de empregados, acesso negado:

```
docker compose exec postgres psql -U vagas_svc -d empregados -c "select 1"
psql: error: ... FATAL:  permission denied for database "empregados"
DETAIL:  User does not have CONNECT privilege.
```

## Limites desta evidência

- Comprova a infraestrutura (Kafka, Postgres, Redis) e o isolamento de acesso entre bancos.
- Não comprova a execução dos microsserviços, que ainda não foram implementados.
- Execução a partir de clone limpo: pendente.