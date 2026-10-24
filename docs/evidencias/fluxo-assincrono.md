# Fluxo assíncrono — acompanhamento por etapa

Data: 2026-10-24.

## Etapa 1 — integração HTTP e snapshots

Implementado: resolução dos IDs numéricos para UUID, consulta de perfil/critérios, validação de identidade e status ABERTA e persistência do evento com snapshots na mesma linha da candidatura. A migração V2 preserva inscrições antigas sem snapshot.

Testes: `DadosCandidaturaClientTest` usa um servidor HTTP local na JVM para verificar sucesso, referência ausente, vaga fechada, falha remota, timeout e identidade inconsistente. `CandidaturasPersistenceTest` verifica snapshots persistidos, duplicata e ausência de gravação após falhas nas consultas.

Validação executada pela IA: `mvn -o -pl candidaturas-service -am clean verify`, com `BUILD SUCCESS` e nove testes de Candidaturas aprovados (seis HTTP e três de persistência). A execução valida o módulo e suas dependências sem Docker; não é um teste contra os serviços reais nem um Pact.

Ainda não há envio ao broker nesta etapa. A existência do snapshot não conclui produtor/consumidor/idempotência.

## Etapa 2 — outbox e publicador Kafka

Implementado: migração V3, gravação de candidatura/outbox na mesma transação e publicação de até 20 pendências por ciclo, com chave `candidaturaId`, payload persistido e tópico `candidaturas.realizadas`. A marcação ocorre após confirmação do Kafka. Falhas mantêm o evento pendente para nova tentativa; o dispatcher interrompe o lote na primeira falha para não multiplicar esperas durante indisponibilidade.

O produtor usa strings JSON, `acks=all` e idempotência do cliente Kafka; isso não elimina a possibilidade de reenvio após falha entre ACK e marcação no banco. Inscrições históricas não são preenchidas nem publicadas retroativamente. A outbox não deve ser limpa enquanto houver eventos pendentes.

`OutboxPublicacaoTest` usa H2 e simula a confirmação/falha do Kafka para verificar marcação após ACK, reenvio com payload estável e rollback da inscrição quando a escrita da outbox falha. Esses testes não comprovam comunicação com um broker; essa verificação pertence à etapa 3.

Validação executada pela IA: `mvn -o -pl candidaturas-service -am clean verify`, `BUILD SUCCESS`, 12 testes de Candidaturas aprovados (nove da etapa 1 e três de outbox).
