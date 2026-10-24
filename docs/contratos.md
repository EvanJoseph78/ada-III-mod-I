# Contratos de integração

Os DTOs e eventos estão em `shared-contracts`. Qualquer mudança passa pelo responsável pelo módulo.

## HTTP

| Serviço | Rota | Resposta |
| --- | --- | --- |
| empregados-service | `GET /api/v1/empregados/{empregadoId}/perfil` | Implementado: `PerfilProfissionalDto`; UUID do empregado (404 se não existir) |
| vagas-service | `GET /api/v1/vagas/{vagaId}/criterios` | Implementado: `CriteriosVagaDto`; UUID da vaga (404 se não existir) |
| vagas-service | `POST /api/v1/vagas/{vagaId}/resultado` | Planejado: publica o resultado e emite `ResultadoPublicado` (409 se a transição for inválida) |

As consultas básicas existentes usam matrícula (`GET /api/v1/empregados/{matriculaEmpregado}`) e ID numérico da vaga (`GET /api/v1/vagas/{idVaga}`). Os cadastros retornam também os UUIDs usados nos contratos. Não houve alteração das classes de `shared-contracts`.

Empregados retorna `empregadoId`, `matriculaEmpregado`, `nomeEmpregado` e `itens`; os nomes provisórios `matricula` e `nome` da implementação intermediária foram substituídos por essa convenção. As rotas `GET /api/v1/empregados/{matriculaEmpregado}/curriculo-profissional` e `/curriculo-educacional` usam a mesma resposta, filtrando `itens`: experiências internas/externas na primeira, cursos/diplomas/certificações na segunda. Ambas retornam 404 para matrícula inexistente.

`POST /api/v1/candidaturas` recebe `matriculaEmpregado` e `idVaga` numéricos e persiste o registro, com UUID `candidaturaId` e data. `GET /api/v1/candidaturas/{candidaturaId}` consulta esse registro. Duplicata de matrícula/vaga retorna 409. A resolução das referências, validação de existência/abertura da vaga e captura dos snapshots para o evento ainda serão implementadas na integração entre serviços.

## Eventos Kafka

Os eventos abaixo estão definidos nos contratos; os produtores e consumidores ainda não foram implementados.

| Tópico | Evento | Produtor | Consumidor | Chave |
| --- | --- | --- | --- | --- |
| `candidaturas.realizadas` | `CandidaturaRealizada` | candidaturas-service | candidaturas-service (avaliação) | `candidaturaId` |
| `avaliacoes.concluidas` | `AvaliacaoConcluida` | candidaturas-service | candidaturas-service (ranking e cache) | `candidaturaId` |
| `vagas.resultados` | `ResultadoPublicado` | vagas-service | candidaturas-service | `vagaId` |

Garantia de entrega: at-least-once. Os consumidores precisam ser idempotentes.
