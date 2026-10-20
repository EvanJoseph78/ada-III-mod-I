# Arquitetura planejada

Status: proposta; implementação pendente.

| Contexto | Responsabilidade | Dados próprios |
| --- | --- | --- |
| Empregados | Perfil e qualificações | Empregados, cursos, certificações e experiências |
| Vagas | Oportunidades e critérios | Vagas, requisitos e resultados publicados |
| Candidaturas | Inscrição e avaliação | Candidaturas, pontuação e ranking |

Cada serviço terá persistência exclusiva, sem consultas diretas às bases dos demais.

## Fluxo a detalhar

Inscrição → evento de candidatura no Kafka → avaliação assíncrona → evento de avaliação concluída → publicação de resultado conforme o ciclo da vaga.

Antes de implementar, definir produtores e consumidores de cada evento, contratos, obtenção dos dados profissionais e critérios, snapshots para cálculo reproduzível, garantia de entrega, atomicidade entre gravação e publicação e chave de idempotência. O fluxo entre serviços coberto pelo contrato executável precisa ser explicitado.

## Infraestrutura e qualidade

Perfil A (Docker) confirmado. Kafka previsto no escopo. Banco e cache ainda pendentes. Compose será criado após essas escolhas.

Planejar cache com TTL, invalidação e fallback, consumidor idempotente, retry limitado com backoff, DLQ e testes de falhas. A rubrica exige contrato executável e testes JVM sem Docker; separar esses testes daqueles que dependem de contêineres.
