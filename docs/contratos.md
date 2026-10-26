# Contratos de integração

Os DTOs e eventos estão em `shared-contracts`. Qualquer mudança passa pelo responsável pelo módulo.

## Contrato executável implementado

Candidaturas consome Vagas por HTTP. `VagasContractConsumerTest` exercita `DadosCandidaturaClient` e gera o Pact das consultas por ID numérico e critérios por UUID. `VagasContractProviderTest` verifica o [JSON versionado](../shared-contracts/pacts/candidaturas-service-vagas-service.json) contra a aplicação Vagas real com H2. Ambos rodam em `mvn verify`, sem Docker; há comparação automática entre JSON gerado e versionado. [Como atualizar](../shared-contracts/pacts/README.md), [evidências](evidencias/contrato-http-vagas.md) e [ADR-007](adr/ADR-007-contrato-http-vagas.md).

Este par atende ao requisito de um contrato executável entre dois serviços. O plano de mensagem da ADR-002 foi substituído para essa primeira entrega. A publicação de `ResultadoPublicado` está implementada com testes Kafka e execução local; ainda não há Pact de mensagem ou dos novos endpoints de coordenação.

## HTTP

| Serviço | Rota | Resposta |
| --- | --- | --- |
| empregados-service | `GET /api/v1/empregados/{empregadoId}/perfil` | Implementado: `PerfilProfissionalDto`; UUID do empregado (404 se não existir) |
| vagas-service | `GET /api/v1/vagas/{vagaId}/criterios` | Implementado: `CriteriosVagaDto`; UUID da vaga (404 se não existir) |
| vagas-service | `POST /api/v1/vagas/{vagaId}/encerramento` | 200, ABERTA → EM_AVALIACAO; repetível em EM_AVALIACAO |
| vagas-service | `POST /api/v1/vagas/{vagaId}/resultado` | 202, intenção durável em outbox; exige fechamento e todas as avaliações; 409 para transição inválida/pendência |
| vagas-service | `POST /api/v1/vagas/{vagaId}/conclusao` | 200, RESULTADO_PUBLICADO → CONCLUIDA; conclusão administrativa |
| candidaturas-service | `GET /api/v1/candidaturas/resultados/{vagaId}` | Publicação original, ranking definitivo e contagens; 404 até existir |

As consultas básicas existentes usam matrícula (`GET /api/v1/empregados/{matriculaEmpregado}`) e ID numérico da vaga (`GET /api/v1/vagas/{idVaga}`). Os cadastros retornam também os UUIDs usados nos contratos. Não houve alteração das classes de `shared-contracts`.

Empregados retorna `empregadoId`, `matriculaEmpregado`, `nomeEmpregado` e `itens`; os nomes provisórios `matricula` e `nome` da implementação intermediária foram substituídos por essa convenção. As rotas `GET /api/v1/empregados/{matriculaEmpregado}/curriculo-profissional` e `/curriculo-educacional` usam a mesma resposta, filtrando `itens`: experiências internas/externas na primeira, cursos/diplomas/certificações na segunda. Ambas retornam 404 para matrícula inexistente.

`POST /api/v1/candidaturas` recebe `matriculaEmpregado` e `idVaga` numéricos, resolve UUIDs por HTTP e consulta perfil/critérios. Retorna 201 ao persistir a inscrição com snapshots; duplicata ou vaga não aberta retorna 409, referência inexistente retorna 422, falha/timeout remoto retorna 503 e resposta inconsistente retorna 502. Nenhuma inscrição é gravada quando essas consultas falham. `GET /api/v1/candidaturas/{candidaturaId}` consulta o registro. Inscrições antigas sem snapshot continuam consultáveis e não são publicadas retroativamente.

### Ranking parcial

`GET /api/v1/candidaturas/ranking/{vagaId}` recebe UUID e retorna `vagaId` e `classificados`, com posição, candidaturaId, empregadoId, matrícula, pontuação e data de inscrição. Inclui apenas avaliados elegíveis; ordem: pontos decrescentes, inscrição crescente e UUID crescente. Sem resultados locais, retorna 200 com lista vazia, sem afirmar existência da vaga. UUID inválido retorna 400. Esse ranking não equivale a resultado publicado.

O ranking usa cache-aside com TTL e revisão por vaga. Uma avaliação confirmada cria outra chave; uma reentrega idempotente não invalida. Em falha do cache, a fonte é consultada; falha da fonte não é ocultada. Estratégia e limites na [ADR-006](adr/ADR-006-cache-ranking.md).

`GET /api/v1/candidaturas/{candidaturaId}` também retorna `status` (`PENDENTE` ou `AVALIADA`), `elegivel` e `pontuacao`. Esses dois últimos campos são nulos antes da avaliação. O snapshot fica congelado na inscrição; alterações futuras de perfil ou critérios não recalculam inscrições existentes. A avaliação usa as regras da [ADR-005](adr/ADR-005-fluxo-assincrono.md).

## Eventos Kafka

`CandidaturaRealizada` é gravado em outbox com a inscrição, publicado por um dispatcher e consumido pelo grupo `avaliacao-curricular`. A avaliação usa o snapshot e persiste uma única vez por candidatura/evento. `ResultadoPublicado` é produzido por Vagas via outbox e consumido pelo grupo `resultados-publicados` de Candidaturas. `AvaliacaoConcluida` permanece somente definido, sem produtor/consumidor; o ranking/cache atuais não dependem dele.

| Tópico | Evento | Produtor | Consumidor | Chave |
| --- | --- | --- | --- | --- |
| `candidaturas.realizadas` | `CandidaturaRealizada` | candidaturas-service | candidaturas-service (avaliação) | `candidaturaId` |
| `avaliacoes.concluidas` (planejado) | `AvaliacaoConcluida` | não implementado | não implementado | `candidaturaId` |
| `vagas.resultados` | `ResultadoPublicado` | vagas-service | candidaturas-service | `vagaId` |

Garantia de entrega: at-least-once. Os consumidores precisam ser idempotentes.

Vagas coordena `POST /api/v1/candidaturas/processos/{vagaId}/encerramento` com body `{"idVaga":4}` (ID numérico real da vaga) e consulta `GET /api/v1/candidaturas/processos/{vagaId}/prontidao`. Ambos retornam `vagaId`, `idVaga`, `inscricoes`, `avaliacoes` e `apto`. Prontidão só existe após fechamento; IDs divergentes/inscrições fechadas retornam 409. Essas rotas são internas por convenção, sem autenticação implementada. Vagas traduz indisponibilidade/timeout para 503 e resposta inconsistente para 502. Não há transação distribuída; repetir fechamento recupera falha parcial.

`ResultadoPublicado` mantém o record original (`eventId`, `ocorridoEm`, `vagaId`), sem mudança no shared-contracts. Após consumo, a consulta definitiva retorna `publicacao`, `ranking`, `inscricoes` e `avaliacoes`. Reentrega igual não altera nada; evento divergente ou JSON inválido segue diretamente para `vagas.resultados.DLT`. Falhas transitórias usam três tentativas totais com o mesmo backoff do consumidor de candidaturas. Confirmação do offset ocorre após a transação. [ADR-008](adr/ADR-008-publicacao-resultado.md) e [evidências](evidencias/publicacao-resultado.md).

No consumidor implementado, o resultado é gravado antes do retorno do listener e da confirmação do offset. Uma reentrega encontra a avaliação existente e não soma pontos novamente. Falhas transitórias têm três tentativas totais; JSON inválido ou evento divergente do snapshot segue diretamente para `candidaturas.realizadas.DLT`. Os tópicos de entrada e DLT têm três partições no ambiente local. Se o envio à DLT falhar, a recuperação não é considerada concluída. O replay da DLT é manual, com os mesmos IDs, chave e payload após corrigir a causa.
