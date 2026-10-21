# ADR-002 — Estratégia de testes: suíte sem Docker e suíte com Docker

Status: aceita
Data: 2026-10-21

## Contexto

A ADR-001 adotou Docker na máquina pessoal como ambiente principal (perfil A) e deixou
em aberto a estratégia de testes. A rubrica exige que o contract test seja executável e que
os testes rodem em um perfil sem Docker (`mvn verify` verde, com testes de integração
puros na JVM). A banca será apresentada na máquina do grupo, e uma falha de Docker nesse
dia não deve impedir a demonstração dos testes.

O projeto precisa comprovar, de forma repetível, quatro comportamentos: contrato entre
serviços, consumidor idempotente, retry com backoff e DLQ para falhas transitórias e
permanentes, e invalidação de cache.

## Decisão

Adotar **duas suítes de testes**, separadas por etiqueta do JUnit 5:

1. **Suíte sem Docker (padrão).** Executada por `mvn verify`. Cobre testes unitários e de
   integração dentro da JVM, usando banco H2 em modo PostgreSQL, Kafka embarcado
   (`spring-kafka-test`), cache em memória no lugar do Redis e Pact para contratos.
2. **Suíte com Docker (etiqueta `docker`).** Usa Testcontainers com Postgres, Redis e Kafka
   reais. Fica excluída do `mvn verify` padrão e é executada somente quando o perfil Maven A
   estiver ativo.

Os cenários de comportamento (idempotência, retry/DLQ, invalidação de cache) são escritos
primeiro na suíte sem Docker. A suíte com Docker repete um conjunto menor de cenários
contra a infraestrutura real, para verificar o que os componentes embarcados não cobrem.

Os contratos executáveis ficam em `shared-contracts/pacts`. O Pact de mensagem cobre
`ResultadoPublicado` (Vagas produz, Candidaturas consome). `CandidaturaRealizada` é interno
ao serviço de Candidaturas e não é objeto de contract test.

## Alternativas consideradas

| Alternativa | Benefícios | Motivo para não ser a escolha |
| --- | --- | --- |
| Somente Testcontainers | Máxima fidelidade à infraestrutura real | Não atende à exigência de testes sem Docker e deixa a demonstração dependente do Docker |
| Somente componentes embarcados | Roda em qualquer máquina, execução rápida | Perde fidelidade em pontos como serialização no Redis e diferenças entre H2 e PostgreSQL |
| Mocks do Kafka e do banco | Rápidos e simples | Não exercitam retry, DLQ nem a restrição única que sustenta a idempotência |
| Testes manuais com `curl` e consoles | Nenhum custo de automação | Não repetíveis, sem evidência verificável e sem proteção contra regressão |

## Consequências e trade-offs

### Benefícios

- `mvn verify` roda em qualquer máquina com JDK e Maven, inclusive sem Docker.
- Os testes de falha e de idempotência são determinísticos e rápidos.
- A suíte com Docker continua disponível para validar o comportamento no ambiente do perfil A.

### Custos e limitações

- Manter duas suítes exige disciplina para que os mesmos cenários não divirjam entre elas.
- H2 em modo PostgreSQL e cache em memória não reproduzem todas as particularidades do
  Postgres e do Redis. Essas diferenças só são cobertas pelos testes etiquetados `docker`.
- O Kafka embarcado pode se comportar de forma ligeiramente diferente do broker
  `apache/kafka:4.1.1`. A compatibilidade das versões precisa ser verificada na implementação.
- Testes que passam na suíte sem Docker não provam, sozinhos, o funcionamento no perfil A.

## Evidências e acompanhamento

- Configuração dos perfis Maven e da exclusão da etiqueta `docker`: `pom.xml` (pendente).
- Contrato executável: `shared-contracts/pacts` e sua verificação (pendente).
- Resultado de `mvn verify` sem Docker registrado em `AVALIACAO.md` (pendente).
- Decisão anterior: [ADR-001](ADR-001-uso-de-docker.md).

Pendências:

- [ ] Configurar o Surefire para excluir a etiqueta `docker` por padrão.
- [ ] Configurar o perfil A para incluir os testes `docker`.
- [ ] Escrever o primeiro teste de idempotência na suíte sem Docker.
- [ ] Escrever o teste de retry (falha transitória) e de DLQ (falha permanente).
- [ ] Verificar o contract test de `ResultadoPublicado`.
- [ ] Registrar o `mvn verify` verde sem Docker na `AVALIACAO.md`.