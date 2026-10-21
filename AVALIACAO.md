# Autoavaliação — Seleção Interna

Grupo e papéis: 3 integrantes — <Evandro>, <André>, <Rafael> (papéis a registrar).

Perfil alvo: A (Docker). Execução da aplicação ainda não comprovada: os serviços ainda não foram implementados.

| Critério | Peso | Estado | Evidência |
| --- | --- | --- | --- |
| 1. Decomposição de domínio | 15 | Em andamento | docs/arquitetura.md (3 contextos com dados próprios); docs/evidencias/execucao-infra.md (um banco e um usuário por serviço, acesso cruzado negado) |
| 2. Comunicação assíncrona | 15 | Em andamento | compose.yaml: broker Kafka local saudável. Produtores e consumidores a implementar |
| 3. Idempotência e consistência | 12 | Pendente | A registrar |
| 4. Cache | 10 | Em andamento | compose.yaml: Redis local saudável. Estratégia e invalidação a implementar |
| 5. Resiliência | 12 | Pendente | A registrar |
| 6. Testabilidade | 13 | Em andamento | docs/adr/ADR-002-estrategia-de-testes.md (estratégia definida). Testes a implementar |
| 7. Decisões arquiteturais | 13 | Em andamento | docs/adr/ADR-001-uso-de-docker.md, docs/adr/ADR-002-estrategia-de-testes.md. Demais ADRs a escrever |
| 8. Uso crítico de IA | 5 | Em registro | docs/uso-de-ia.md; revisão humana pendente |
| 9. Execução comprovada | 5 | Parcial | docs/evidencias/execucao-infra.md: Kafka, Postgres e Redis saudáveis no perfil A. Aplicação ainda não existe; clone limpo pendente |


## Opcionais entregues

Nenhum. O grupo tem 3 integrantes, então entrega apenas o CORE; opcionais não são exigidos.