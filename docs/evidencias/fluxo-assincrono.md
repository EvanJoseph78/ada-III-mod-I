# Fluxo assíncrono — acompanhamento por etapa

Data: 2026-10-24.

## Etapa 1 — integração HTTP e snapshots

Implementado: resolução dos IDs numéricos para UUID, consulta de perfil/critérios, validação de identidade e status ABERTA e persistência do evento com snapshots na mesma linha da candidatura. A migração V2 preserva inscrições antigas sem snapshot.

Testes: `DadosCandidaturaClientTest` usa um servidor HTTP local na JVM para verificar sucesso, referência ausente, vaga fechada, falha remota, timeout e identidade inconsistente. `CandidaturasPersistenceTest` verifica snapshots persistidos, duplicata e ausência de gravação após falhas nas consultas.

Validação executada pela IA: `mvn -o -pl candidaturas-service -am clean verify`, com `BUILD SUCCESS` e nove testes de Candidaturas aprovados (seis HTTP e três de persistência). A execução valida o módulo e suas dependências sem Docker; não é um teste contra os serviços reais nem um Pact.

Ainda não há envio ao broker nesta etapa. A existência do snapshot não conclui produtor/consumidor/idempotência.
