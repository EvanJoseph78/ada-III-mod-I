# Contratos de integração

Os DTOs e eventos estão em `shared-contracts`. Qualquer mudança passa pelo responsável pelo módulo.

## HTTP

| Serviço | Rota | Resposta |
| --- | --- | --- |
| empregados-service | `GET /empregados/{id}/perfil` | `PerfilProfissionalDto` (404 se não existir) |
| vagas-service | `GET /vagas/{id}/criterios` | `CriteriosVagaDto` (404 se não existir) |
| vagas-service | `POST /vagas/{id}/resultado` | Publica o resultado e emite `ResultadoPublicado` (409 se a transição for inválida) |

## Eventos Kafka

| Tópico | Evento | Produtor | Consumidor | Chave |
| --- | --- | --- | --- | --- |
| `candidaturas.realizadas` | `CandidaturaRealizada` | candidaturas-service | candidaturas-service (avaliação) | `candidaturaId` |
| `avaliacoes.concluidas` | `AvaliacaoConcluida` | candidaturas-service | candidaturas-service (ranking e cache) | `candidaturaId` |
| `vagas.resultados` | `ResultadoPublicado` | vagas-service | candidaturas-service | `vagaId` |

Garantia de entrega: at-least-once. Os consumidores precisam ser idempotentes.