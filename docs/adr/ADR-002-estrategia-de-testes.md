# ADR-002 — Estratégia de testes: suíte sem Docker e suíte com Docker

Status: aceita
Data: 2026-10-21

## Contexto

A [ADR-001](ADR-001-uso-de-docker.md) adotou Docker na máquina pessoal como ambiente principal (perfil A) e deixou em aberto a estratégia de testes. A rubrica exige que o contract test seja executável e que os testes rodem em um perfil sem Docker (`mvn verify` verde, com testes de integração puros na JVM). A banca será apresentada na máquina do grupo, e uma falha de Docker nesse dia não deve impedir a demonstração dos testes.

O projeto precisa comprovar, de forma repetível, quatro comportamentos: contrato entre serviços, consumidor idempotente, retry com backoff e DLQ para falhas transitórias e permanentes, e invalidação de cache.

## Decisão

Adotar **duas suítes de testes**, separadas por etiqueta do JUnit 5:

1. **Suíte sem Docker (padrão).** Cobre testes unitários e de integração dentro da JVM, usando banco H2 em modo PostgreSQL, Kafka embarcado (`spring-kafka-test`), cache em memória com Caffeine (via Spring Cache) no lugar do Redis e Pact para contratos.
2. **Suíte com Docker (etiqueta `docker`).** Usa Testcontainers com PostgreSQL, Redis e Kafka reais. Os testes dessa suíte levam a anotação `@Tag("docker")` e ficam excluídos da execução padrão.

**Como executar:**

- `mvn verify`, sem perfil (ou com o perfil B), executa somente a suíte sem Docker.
- `mvn verify -PA` executa também os testes etiquetados `docker`.

**Convenção de execução:** todos os testes usam o sufixo `Test` e rodam no Surefire. A etiqueta `docker` é excluída por padrão via `excludedGroups` e incluída pelo perfil A. Não são usados testes com sufixo `IT` nem o plugin Failsafe, para que a exclusão fique em um único lugar.

Os cenários de comportamento (idempotência, retry/DLQ, invalidação de cache) são escritos primeiro na suíte sem Docker. A suíte com Docker repete um conjunto menor de cenários contra a infraestrutura real, para verificar o que os componentes embarcados não cobrem.

**Contratos executáveis.** O plano inicial era o Pact de mensagem de `ResultadoPublicado`. A [ADR-007](ADR-007-contrato-http-vagas.md) substitui essa escolha para o primeiro contrato: Candidaturas usa seu cliente HTTP real para gerar expectativas das consultas de vaga/critérios, e Vagas verifica o JSON versionado contra sua aplicação real com H2. A geração ocorre em `target/pacts`, a cópia revisada fica em `shared-contracts/pacts`, sem Pact Broker ou Docker. Publicação do resultado e Pact de mensagem continuam fora da implementação atual.

`CandidaturaRealizada` é interno ao serviço de Candidaturas e não é objeto de contract test. O primeiro par executável cobre `GET /api/v1/vagas/{idVaga}` e `GET /api/v1/vagas/{vagaId}/criterios`.

## Alternativas consideradas

| Alternativa | Benefícios | Motivo para não ser a escolha |
| --- | --- | --- |
| Somente Testcontainers | Máxima fidelidade à infraestrutura real | Não atende à exigência de testes sem Docker e deixa a demonstração dependente do Docker |
| Somente componentes embarcados | Roda em qualquer máquina, execução rápida | Perde fidelidade em pontos como serialização no Redis e diferenças entre H2 e PostgreSQL |
| Mocks do Kafka e do banco | Rápidos e simples | Não exercitam retry, DLQ nem a restrição única que sustenta a idempotência |
| Testes manuais com `curl` e consoles | Nenhum custo de automação | Não repetíveis, sem evidência verificável e sem proteção contra regressão |
| Spring Cloud Contract no lugar do Pact | Integrado ao ecossistema Spring, gera stubs a partir do produtor | O contrato nasce no produtor; o Pact parte do consumidor e deixa explícito o que Candidaturas realmente usa de `ResultadoPublicado` |

## Consequências e trade-offs

### Benefícios

- `mvn verify` roda em qualquer máquina com JDK e Maven, inclusive sem Docker.
- Os testes de falha e de idempotência são determinísticos e rápidos.
- A suíte com Docker continua disponível para validar o comportamento no ambiente do perfil A.
- O contrato entre Vagas e Candidaturas fica versionado e verificável sem infraestrutura externa.

### Custos e limitações

- Manter duas suítes exige disciplina para que os mesmos cenários não divirjam entre elas.
- H2 em modo PostgreSQL e Caffeine não reproduzem todas as particularidades do PostgreSQL e do Redis. Essas diferenças só são cobertas pelos testes etiquetados `docker`.
- O Kafka embarcado pode se comportar de forma ligeiramente diferente do broker `apache/kafka:4.1.1`. A compatibilidade das versões depende da versão do Spring Boot adotada e precisa ser verificada na implementação.
- Testes que passam na suíte sem Docker não provam, sozinhos, o funcionamento no perfil A.
- Sem Pact Broker, a sincronia entre o arquivo de pact e o produtor depende de commitar o arquivo gerado a cada mudança de contrato.

## Evidências e acompanhamento

- Configuração dos perfis Maven e da exclusão da etiqueta `docker`: `pom.xml` (pendente).
- Contrato executável: JSON em `shared-contracts/pacts`, testes de consumidor/provedor e proteção contra divergência implementados; [evidências](../evidencias/contrato-http-vagas.md).
- Primeiro conjunto implementado: seis testes HTTP/JPA com H2, migrações Flyway e validação de schema; resultado sem Docker em [persistencia.md](../evidencias/persistencia.md) e `AVALIACAO.md`. Os demais componentes da estratégia continuam pendentes.
- Evolução em 2026-10-24: 30 testes no build completo, incluindo 25 de Candidaturas com HTTP, H2, outbox, cálculo, concorrência e Kafka KRaft embarcado. Resultado em [fluxo-assincrono.md](../evidencias/fluxo-assincrono.md). Pact, cache e suíte Testcontainers permanecem pendentes.
- Após o cache, o build completo passou com 43 testes, incluindo Caffeine com relógio controlado, invalidação, rollback/concorrência/fallback e serialização Redis. Validação com Redis local registrada em [cache-ranking.md](../evidencias/cache-ranking.md). Cache está concluído; Pact e suíte Testcontainers continuam pendentes.
- Decisão anterior: [ADR-001](ADR-001-uso-de-docker.md).

Pendências:

- [x] Definir Spring Boot 3.5.9, Spring Kafka 3.3.11 e cliente/broker embarcado 3.9.1; verificar publicação/consumo no Kafka local. O Compose define a imagem 4.1.1; a versão binária do broker local não foi interrogada diretamente nesta sessão.
- [ ] Configurar o Surefire para excluir a etiqueta `docker` por padrão.
- [ ] Configurar o perfil A para incluir os testes `docker`.
- [x] Escrever teste de idempotência e concorrência sem Docker, incluindo reentrega pelo Kafka.
- [x] Escrever teste de retry transitório, esgotamento de tentativas e mensagem inválida para DLT (a DLQ deste fluxo).
- [x] Gerar e verificar um contrato entre dois serviços: Pact HTTP Candidaturas → Vagas, conforme ADR-007. Pact de `ResultadoPublicado` permanece uma possível evolução.
