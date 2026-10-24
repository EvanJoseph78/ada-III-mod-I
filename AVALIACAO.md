# Autoavaliação — Seleção Interna

Grupo e papéis: 3 integrantes — André, Evandro e Rafael (papéis a registrar).

Perfil alvo: A (Docker). Infraestrutura, persistência e fluxo de inscrição/avaliação possuem evidências de execução. Ranking/cache, publicação do resultado, Pact e execução a partir de clone limpo continuam pendentes. Os profiles Maven A/B/C continuam como marcadores.

| Critério | Peso | Estado | Evidência |
| --- | --- | --- | --- |
| 1. Decomposição de domínio | 15 | Em andamento | [Arquitetura](docs/arquitetura.md) e [ADR-004](docs/adr/ADR-004-persistencia-por-servico.md): entidades, repositórios e migrações próprios por serviço. [Infraestrutura](docs/evidencias/execucao-infra.md): segregação dos bancos. [Testes H2 e validação PostgreSQL](docs/evidencias/persistencia.md) aprovados, inclusive após reiniciar os três serviços |
| 2. Comunicação assíncrona | 15 | Implementada no fluxo de candidatura | [ADR-005](docs/adr/ADR-005-fluxo-assincrono.md) e [fluxo assíncrono](docs/evidencias/fluxo-assincrono.md): outbox, produtor, listener Kafka e avaliação persistida; testes e execução local aprovados |
| 3. Idempotência e consistência | 12 | Implementada na avaliação | [Fluxo assíncrono](docs/evidencias/fluxo-assincrono.md): resultado único, bloqueio transacional, reentrega/concorrência e reenvio após reiniciar a aplicação sem duplicar; atomicidade da outbox testada |
| 4. Cache | 10 | Em validação | [ADR-006](docs/adr/ADR-006-cache-ranking.md): cache-aside, TTL e invalidação lógica por revisão. [Testes de ranking/cache](docs/evidencias/cache-ranking.md) aprovados com Caffeine; TTL, falhas/concorrência e Redis real seguem na etapa 3 |
| 5. Resiliência | 12 | Parcial | [Fluxo assíncrono](docs/evidencias/fluxo-assincrono.md): timeouts HTTP, retomada da outbox, retry exponencial e DLT; testes de falha transitória, persistente e JSON inválido. Operação de replay manual e demais fluxos a evoluir |
| 6. Testabilidade | 13 | Em andamento | [ADR-002](docs/adr/ADR-002-estrategia-de-testes.md): estratégia definida. [Persistência](docs/evidencias/persistencia.md): 30 testes no build completo sem Docker, incluindo Kafka KRaft, idempotência e falhas conforme [fluxo assíncrono](docs/evidencias/fluxo-assincrono.md). Pact, cache e suíte Testcontainers pendentes |
| 7. Decisões arquiteturais | 13 | Em andamento | [ADR-001](docs/adr/ADR-001-uso-de-docker.md), [ADR-002](docs/adr/ADR-002-estrategia-de-testes.md), [ADR-003](docs/adr/ADR-003-heranca-maven.md), [ADR-004](docs/adr/ADR-004-persistencia-por-servico.md) e [ADR-005](docs/adr/ADR-005-fluxo-assincrono.md). Cache e publicação de resultado a registrar |
| 8. Uso crítico de IA | 5 | Em registro | [Uso de IA](docs/uso-de-ia.md): sugestão sobre Maven, decisão humana, limitação de escopo e validação pela IA. Revisão final do grupo pendente |
| 9. Execução comprovada | 5 | Parcial | [Infraestrutura](docs/evidencias/execucao-infra.md): Kafka, PostgreSQL e Redis saudáveis no perfil A. [Persistência PostgreSQL](docs/evidencias/persistencia.md): três serviços iniciados e dados recuperados após reinício das aplicações; [inscrição/avaliação por HTTP/Kafka](docs/evidencias/fluxo-assincrono.md) comprovada com reentrega após reinício. Fluxo de ranking/publicação e clone limpo pendentes |

## Validação do build — 2026-10-22

Após a padronização dos POMs, foi executado na raiz:

```sh
mvn -o clean verify
```

Resultado: `BUILD SUCCESS` para `selecao-interna`, `empregados-service`, `vagas-service`, `candidaturas-service` e `shared-contracts`. A execução ocorreu sem Docker e utilizou dependências previamente disponíveis no repositório Maven local. Em uma máquina sem essas dependências, executar `mvn clean verify` com acesso aos repositórios configurados.

Essa verificação foi executada pela IA e comprova compilação e empacotamento. Na versão do ajuste dos POMs ainda não havia testes automatizados; o resultado não comprovava contrato executável, idempotência, cache ou resiliência.

## Evolução — persistência mínima

Após o ajuste dos POMs, foram implementados cadastros persistentes de perfil, critérios de vaga e candidatura. `mvn clean verify` passou com seis testes de integração sem Docker, aplicando migrações Flyway e validando o schema no H2. Ver [resultado, limites e roteiro PostgreSQL](docs/evidencias/persistencia.md). Na integração de 2026-10-24, o build passou com sete testes, e os três serviços foram validados com PostgreSQL 17.11, inclusive após reiniciar as aplicações. A integração HTTP/Kafka de inscrição e avaliação foi concluída nas etapas abaixo; ranking, cache e teste de contrato continuam pendentes.

## Evolução — fluxo assíncrono por etapas (2026-10-24)

Etapa 1: Candidaturas resolve IDs, valida referências e vaga ABERTA por HTTP e persiste snapshots do evento. Falhas de consulta não gravam a inscrição. A implementação e seus testes estão em [fluxo-assincrono.md](docs/evidencias/fluxo-assincrono.md), com decisão na [ADR-005](docs/adr/ADR-005-fluxo-assincrono.md). Essa etapa isolada ainda não conclui produtor, consumidor ou idempotência.

Etapa 2: produtor via outbox transacional implementado, com 12 testes do módulo aprovados. Há testes de rollback conjunto, falha/retomada de envio e confirmação após ACK. Consumidor e testes com broker são a próxima etapa; o requisito assíncrono ainda não é considerado concluído.

Etapa 3: avaliação e deduplicação transacionais implementadas, com resultado consultável e testes de reentrega/concorrência; 21 testes do módulo aprovados. A conexão do consumidor ao Kafka e a verificação com broker seguem na etapa 4, antes de marcar o requisito completo.

Etapa 4: listener Kafka, retry/DLT e verificação ponta a ponta concluídos. `mvn -o clean verify` passou com 30 testes; execução real com os três serviços, PostgreSQL e Kafka confirmou avaliação e reentrega após reiniciar Candidaturas. O requisito de um fluxo assíncrono com produtor, consumidor e idempotência está atendido nesta branch. Ranking/cache e Pact permanecem pendentes.

## Evolução — ranking e cache por etapas

Etapa 1: endpoint de ranking parcial implementado, com dois testes de filtro/ordenação e resposta HTTP aprovados. Essa etapa ainda não utiliza cache. Decisão na [ADR-006](docs/adr/ADR-006-cache-ranking.md) e evidência em [cache-ranking.md](docs/evidencias/cache-ranking.md).

Etapa 2: Redis/Caffeine, cache-aside e chave por revisão implementados; seis testes direcionados aprovados. A contagem das avaliações muda a chave no commit e evita depender de um evento de invalidação separado. Validação de falhas/TTL e Redis real ainda pendente.

## Opcionais (grupo de três integrantes)

Nenhum. O grupo tem três integrantes, então entrega apenas o CORE; opcionais não são exigidos.
