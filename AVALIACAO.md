# Autoavaliação — Seleção Interna

Grupo e papéis: 3 integrantes — André, Evandro e Rafael (papéis a registrar).

Perfil de referência: A, com infraestrutura Docker e aplicações JVM conforme ADR-001. Verificação efetiva: três JARs contra dependências locais, com fluxo até CONCLUIDA e reinícios; não se afirma recriação de contêineres nessa sessão. Suíte B/padrão sem Docker aprovada com **76 testes**, usando H2/Caffeine/Kafka embarcado/Pact. A/B configuram filtros de testes; não iniciam infraestrutura. [Clone local limpo e execução com seus JARs](docs/evidencias/entrega-final.md) aprovados. Revisão humana e ensaio do grupo não foram realizados pela IA.

| Critério | Peso | Estado | Evidência |
| --- | --- | --- | --- |
| 1. Decomposição de domínio | 15 | Implementada | [Arquitetura](docs/arquitetura.md), [ADR-004](docs/adr/ADR-004-persistencia-por-servico.md), [infraestrutura](docs/evidencias/execucao-infra.md) e [persistência](docs/evidencias/persistencia.md): entidades/migrações próprias, bancos e credenciais segregados; sem consulta direta ao banco de outro serviço |
| 2. Comunicação assíncrona | 15 | Implementada no fluxo de candidatura | [ADR-005](docs/adr/ADR-005-fluxo-assincrono.md) e [fluxo assíncrono](docs/evidencias/fluxo-assincrono.md): outbox, produtor, listener Kafka e avaliação persistida; testes e execução local aprovados |
| 3. Idempotência e consistência | 12 | Implementada na avaliação | [Fluxo assíncrono](docs/evidencias/fluxo-assincrono.md): resultado único, bloqueio transacional, reentrega/concorrência e reenvio após reiniciar a aplicação sem duplicar; atomicidade da outbox testada |
| 4. Cache | 10 | Implementado no ranking | [ADR-006](docs/adr/ADR-006-cache-ranking.md): cache-aside, TTL e invalidação lógica por revisão. [Evidência](docs/evidencias/cache-ranking.md): 13 testes de ranking/cache, concorrência/rollback/fallback e Redis 7.4.11 real aprovados |
| 5. Resiliência | 12 | Implementada no recorte | [Fluxo assíncrono](docs/evidencias/fluxo-assincrono.md) e [publicação](docs/evidencias/publicacao-resultado.md): timeouts HTTP, duas outboxes, retry exponencial e DLT; falhas transitórias/permanentes, retomada após reinício e operação manual de replay. Não há circuit breaker/alta disponibilidade |
| 6. Testabilidade | 13 | Contrato executável implementado | [ADR-007](docs/adr/ADR-007-contrato-http-vagas.md) e [evidência Pact](docs/evidencias/contrato-http-vagas.md): consumidor com cliente real, sete verificações contra API/JPA real do provedor e proteção contra JSON desatualizado; build sem Docker. Testcontainers continua pendente, sem ser requisito CORE isolado |
| 7. Decisões arquiteturais | 13 | Documentadas | [ADRs 001–008](docs/adr/README.md), com alternativas e custos. [ADR-008](docs/adr/ADR-008-publicacao-resultado.md): barreira, contenção, fechamento monotônico, outbox e consistência eventual. Revisão final do grupo pendente |
| 8. Uso crítico de IA | 5 | Em registro | [Uso de IA](docs/uso-de-ia.md): sugestão sobre Maven, decisão humana, limitação de escopo e validação pela IA. Revisão final do grupo pendente |
| 9. Execução comprovada | 5 | Fluxo completo comprovado | [Publicação](docs/evidencias/publicacao-resultado.md): inscrições → avaliação → ranking/cache → fechamento → publicação → resultado definitivo → conclusão; três JARs, PostgreSQL/Kafka/Redis locais e reinícios. [Helper reproduzível](scripts/validar-fluxo.ps1), além das evidências anteriores de [infraestrutura](docs/evidencias/execucao-infra.md), [cache](docs/evidencias/cache-ranking.md) e [Pact](docs/evidencias/contrato-http-vagas.md) |

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

Etapa 3: build completo aprovado com 43 testes. Concorrência, expiração, rollback e fallback testados; Redis 7.4.11 real confirmou preenchimento/hit, revisão após novas avaliações, TTL e fallback de conexão/JSON. O requisito de cache com estratégia e invalidação explícitas está atendido nesta branch. As pendências da etapa anterior foram concluídas; publicação do resultado/Pact e clone limpo permanecem.

## Evolução — contrato executável HTTP

O primeiro contrato obrigatório está implementado para Candidaturas → Vagas, conforme [ADR-007](docs/adr/ADR-007-contrato-http-vagas.md), substituindo a previsão inicial de mensagem da ADR-002. O cliente real do consumidor gera expectativas e Vagas verifica sete interações contra sua API/JPA reais com H2. O JSON versionado é comparado com o gerado; incompatibilidade foi introduzida temporariamente, detectada e restaurada. `mvn -o clean verify` passou com **54 testes**. [Evidências e limites](docs/evidencias/contrato-http-vagas.md). Publicação de resultado, clone limpo, revisão humana final e ensaio continuam pendentes.

## Evolução — fechamento e publicação completos

A [ADR-008](docs/adr/ADR-008-publicacao-resultado.md) implementa fechamento com barreira persistente, verificação de todas as avaliações, status/outbox atômicos em Vagas, consumo idempotente e snapshot definitivo em Candidaturas. CONCLUIDA é administrativa, sem confirmar consumo remoto. O cache parcial permanece separado e o contrato compartilhado do evento não mudou. [Evidência](docs/evidencias/publicacao-resultado.md): 76 testes no perfil B e execução real com retomada após reinícios. Não há reabertura/retificação, segurança de produção ou Testcontainers. Restam revisão humana de papéis/autoavaliação, ensaio e preparação final da banca.

## Opcionais (grupo de três integrantes)

Nenhum. O grupo tem três integrantes, então entrega apenas o CORE; opcionais não são exigidos.
