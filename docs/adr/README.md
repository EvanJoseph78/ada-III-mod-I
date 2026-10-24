# Decisões arquiteturais

Usar TEMPLATE.md para cada decisão. Registrar alternativas e trade-offs reais.

## Decisões registradas

- [ADR-001 - Uso de Docker no ambiente do projeto](ADR-001-uso-de-docker.md) — aceita; 
- [ADR-002 - Estrategia de testes](ADR-002-estrategia-de-testes.md) — aceita; 

- [ADR-003 - Herança Maven e configuração comum do build](ADR-003-heranca-maven.md) — aceita e implementada.
- [ADR-004 - Persistência e limites de dados por serviço](ADR-004-persistencia-por-servico.md) — aceita; integração na develop, sete testes H2 e validação PostgreSQL aprovados.
- [ADR-005 - Snapshots, outbox e avaliação idempotente](ADR-005-fluxo-assincrono.md) — aceita e implementada; testes Kafka e verificação com infraestrutura local aprovados.

## Decisões pendentes

Decisões a elaborar:

1. Evolução dos limites de domínio da ADR-004 conforme a integração entre serviços.
2. Publicação de resultado e contrato executável entre serviços; entrega e idempotência da candidatura estão na ADR-005.
3. Cache: tecnologia, TTL, invalidação e fallback.
4. Implementação e validação da estratégia de testes da ADR-002.

As decisões pendentes acima ainda precisam ser discutidas e registradas em ADRs próprias.
