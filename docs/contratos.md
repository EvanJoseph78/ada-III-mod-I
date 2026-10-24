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

`POST /api/v1/candidaturas` recebe `matriculaEmpregado` e `idVaga` numéricos, resolve UUIDs por HTTP e consulta perfil/critérios. Retorna 201 ao persistir a inscrição com snapshots; duplicata ou vaga não aberta retorna 409, referência inexistente retorna 422, falha/timeout remoto retorna 503 e resposta inconsistente retorna 502. Nenhuma inscrição é gravada quando essas consultas falham. `GET /api/v1/candidaturas/{candidaturaId}` consulta o registro. Inscrições antigas sem snapshot continuam consultáveis e não são publicadas retroativamente.

## Eventos Kafka

`GET /api/v1/candidaturas/{candidaturaId}` também retorna `status` (`PENDENTE` ou `AVALIADA`), `elegivel` e `pontuacao`. Esses dois últimos campos são nulos antes da avaliação. O snapshot fica congelado na inscrição; alterações futuras de perfil ou critérios não recalculam inscrições existentes. A avaliação usa as regras da [ADR-005](adr/ADR-005-fluxo-assincrono.md).

`CandidaturaRealizada` é gravado em outbox com a inscrição, publicado por um dispatcher e consumido pelo grupo `avaliacao-curricular`. A avaliação usa apenas o snapshot, persiste uma única vez por candidatura/evento e fica disponível no GET da inscrição. `AvaliacaoConcluida` e `ResultadoPublicado` continuam planejados, sem produtores/consumidores implementados; consultar [acompanhamento](evidencias/fluxo-assincrono.md).

| Tópico | Evento | Produtor | Consumidor | Chave |
| --- | --- | --- | --- | --- |
| `candidaturas.realizadas` | `CandidaturaRealizada` | candidaturas-service | candidaturas-service (avaliação) | `candidaturaId` |
| `avaliacoes.concluidas` | `AvaliacaoConcluida` | candidaturas-service | candidaturas-service (ranking e cache) | `candidaturaId` |
| `vagas.resultados` | `ResultadoPublicado` | vagas-service | candidaturas-service | `vagaId` |

Garantia de entrega: at-least-once. Os consumidores precisam ser idempotentes.

No consumidor implementado, o resultado é gravado antes do retorno do listener e da confirmação do offset. Uma reentrega encontra a avaliação existente e não soma pontos novamente. Falhas transitórias têm três tentativas totais; JSON inválido ou evento divergente do snapshot segue diretamente para `candidaturas.realizadas.DLT`. Os tópicos de entrada e DLT têm três partições no ambiente local. Se o envio à DLT falhar, a recuperação não é considerada concluída. O replay da DLT é manual, com os mesmos IDs, chave e payload após corrigir a causa.
