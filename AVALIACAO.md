# Autoavaliação — Seleção Interna

Grupo e papéis: 3 integrantes — André, Evandro e Rafael (papéis a registrar).

Perfil alvo: A (Docker). A infraestrutura possui evidência de execução. Os serviços têm implementação inicial, mas o fluxo completo da aplicação no perfil A e a execução a partir de um clone limpo ainda não foram comprovados. Os profiles Maven A/B/C continuam como marcadores.

| Critério | Peso | Estado | Evidência |
| --- | --- | --- | --- |
| 1. Decomposição de domínio | 15 | Em andamento | [Arquitetura](docs/arquitetura.md) e [ADR-004](docs/adr/ADR-004-persistencia-por-servico.md): entidades, repositórios e migrações próprios por serviço. [Infraestrutura](docs/evidencias/execucao-infra.md): segregação dos bancos. [Testes H2 e validação PostgreSQL](docs/evidencias/persistencia.md) aprovados, inclusive após reiniciar os três serviços |
| 2. Comunicação assíncrona | 15 | Em andamento | `compose.yaml` e [evidência de execução](docs/evidencias/execucao-infra.md): Kafka local saudável. Produtores e consumidores a implementar |
| 3. Idempotência e consistência | 12 | Pendente | Implementação e teste de reprocessamento a realizar |
| 4. Cache | 10 | Em andamento | `compose.yaml` e [evidência de execução](docs/evidencias/execucao-infra.md): Redis local saudável. Estratégia, TTL, invalidação e fallback a implementar |
| 5. Resiliência | 12 | Pendente | Implementação e testes de falhas a realizar |
| 6. Testabilidade | 13 | Em andamento | [ADR-002](docs/adr/ADR-002-estrategia-de-testes.md): estratégia definida. [Persistência](docs/evidencias/persistencia.md): sete testes de integração HTTP/JPA/H2 aprovados sem Docker. Pact, idempotência, cache e testes de resiliência pendentes |
| 7. Decisões arquiteturais | 13 | Em andamento | [ADR-001](docs/adr/ADR-001-uso-de-docker.md), [ADR-002](docs/adr/ADR-002-estrategia-de-testes.md), [ADR-003](docs/adr/ADR-003-heranca-maven.md) e [ADR-004](docs/adr/ADR-004-persistencia-por-servico.md). Mensageria, cache e evolução do fluxo a registrar |
| 8. Uso crítico de IA | 5 | Em registro | [Uso de IA](docs/uso-de-ia.md): sugestão sobre Maven, decisão humana, limitação de escopo e validação pela IA. Revisão final do grupo pendente |
| 9. Execução comprovada | 5 | Parcial | [Infraestrutura](docs/evidencias/execucao-infra.md): Kafka, PostgreSQL e Redis saudáveis no perfil A. [Persistência PostgreSQL](docs/evidencias/persistencia.md): três serviços iniciados e dados recuperados após reinício das aplicações; fluxo completo e clone limpo pendentes |

## Validação do build — 2026-10-22

Após a padronização dos POMs, foi executado na raiz:

```sh
mvn -o clean verify
```

Resultado: `BUILD SUCCESS` para `selecao-interna`, `empregados-service`, `vagas-service`, `candidaturas-service` e `shared-contracts`. A execução ocorreu sem Docker e utilizou dependências previamente disponíveis no repositório Maven local. Em uma máquina sem essas dependências, executar `mvn clean verify` com acesso aos repositórios configurados.

Essa verificação foi executada pela IA e comprova compilação e empacotamento. Na versão do ajuste dos POMs ainda não havia testes automatizados; o resultado não comprovava contrato executável, idempotência, cache ou resiliência.

## Evolução — persistência mínima

Após o ajuste dos POMs, foram implementados cadastros persistentes de perfil, critérios de vaga e candidatura. `mvn clean verify` passou com seis testes de integração sem Docker, aplicando migrações Flyway e validando o schema no H2. Ver [resultado, limites e roteiro PostgreSQL](docs/evidencias/persistencia.md). Na integração de 2026-10-24, o build passou com sete testes, e os três serviços foram validados com PostgreSQL 17.11, inclusive após reiniciar as aplicações. O fluxo completo entre serviços e o teste de contrato continuam pendentes.

## Evolução — fluxo assíncrono por etapas (2026-10-24)

Etapa 1: Candidaturas resolve IDs, valida referências e vaga ABERTA por HTTP e persiste snapshots do evento. Falhas de consulta não gravam a inscrição. A implementação e seus testes estão em [fluxo-assincrono.md](docs/evidencias/fluxo-assincrono.md), com decisão na [ADR-005](docs/adr/ADR-005-fluxo-assincrono.md). Essa etapa isolada ainda não conclui produtor, consumidor ou idempotência.

Etapa 2: produtor via outbox transacional implementado, com 12 testes do módulo aprovados. Há testes de rollback conjunto, falha/retomada de envio e confirmação após ACK. Consumidor e testes com broker são a próxima etapa; o requisito assíncrono ainda não é considerado concluído.

Etapa 3: avaliação e deduplicação transacionais implementadas, com resultado consultável e testes de reentrega/concorrência; 21 testes do módulo aprovados. A conexão do consumidor ao Kafka e a verificação com broker seguem na etapa 4, antes de marcar o requisito completo.

## Opcionais do grupo

Nenhum. O grupo tem três integrantes, então entrega apenas o CORE; opcionais não são exigidos.
