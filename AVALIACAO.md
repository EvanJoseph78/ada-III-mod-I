# Autoavaliação — Seleção Interna

Grupo e papéis: 3 integrantes — André, Evandro e Rafael (papéis a registrar).

Perfil alvo: A (Docker). A infraestrutura possui evidência de execução. Os serviços têm implementação inicial, mas o fluxo completo da aplicação no perfil A e a execução a partir de um clone limpo ainda não foram comprovados. Os profiles Maven A/B/C continuam como marcadores.

| Critério | Peso | Estado | Evidência |
| --- | --- | --- | --- |
| 1. Decomposição de domínio | 15 | Em andamento | [Arquitetura](docs/arquitetura.md): três contextos com dados próprios. [Infraestrutura](docs/evidencias/execucao-infra.md): um banco e um usuário por serviço, acesso cruzado negado. Integração dos serviços com persistência pendente |
| 2. Comunicação assíncrona | 15 | Em andamento | `compose.yaml` e [evidência de execução](docs/evidencias/execucao-infra.md): Kafka local saudável. Produtores e consumidores a implementar |
| 3. Idempotência e consistência | 12 | Pendente | Implementação e teste de reprocessamento a realizar |
| 4. Cache | 10 | Em andamento | `compose.yaml` e [evidência de execução](docs/evidencias/execucao-infra.md): Redis local saudável. Estratégia, TTL, invalidação e fallback a implementar |
| 5. Resiliência | 12 | Pendente | Implementação e testes de falhas a realizar |
| 6. Testabilidade | 13 | Em andamento | [ADR-002](docs/adr/ADR-002-estrategia-de-testes.md): estratégia definida. [ADR-003](docs/adr/ADR-003-heranca-maven.md): build validado sem Docker. Testes automatizados, Pact e testes de integração pendentes |
| 7. Decisões arquiteturais | 13 | Em andamento | [ADR-001](docs/adr/ADR-001-uso-de-docker.md), [ADR-002](docs/adr/ADR-002-estrategia-de-testes.md) e [ADR-003](docs/adr/ADR-003-heranca-maven.md). Demais decisões de domínio, mensageria e cache a registrar |
| 8. Uso crítico de IA | 5 | Em registro | [Uso de IA](docs/uso-de-ia.md): sugestão sobre Maven, decisão humana, limitação de escopo e validação pela IA. Revisão final do grupo pendente |
| 9. Execução comprovada | 5 | Parcial | [Infraestrutura](docs/evidencias/execucao-infra.md): Kafka, PostgreSQL e Redis saudáveis no perfil A. Compilação e empacotamento conforme [ADR-003](docs/adr/ADR-003-heranca-maven.md); fluxo completo e clone limpo pendentes |

## Validação do build — 2026-10-22

Após a padronização dos POMs, foi executado na raiz:

```sh
mvn -o clean verify
```

Resultado: `BUILD SUCCESS` para `selecao-interna`, `empregados-service`, `vagas-service`, `candidaturas-service` e `shared-contracts`. A execução ocorreu sem Docker e utilizou dependências previamente disponíveis no repositório Maven local. Em uma máquina sem essas dependências, executar `mvn clean verify` com acesso aos repositórios configurados.

Essa verificação foi executada pela IA e comprova compilação e empacotamento. Como não há testes automatizados nesta versão, ela não comprova os critérios de contrato executável, idempotência, cache ou resiliência.

## Opcionais entregues

Nenhum. O grupo tem três integrantes, então entrega apenas o CORE; opcionais não são exigidos.
