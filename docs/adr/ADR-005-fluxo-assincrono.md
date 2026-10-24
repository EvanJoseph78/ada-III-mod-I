# ADR-005 — Snapshots, outbox e avaliação idempotente

Status: aceita; implementação em etapas
Data: 2026-10-24

## Contexto

O CORE exige produtor, consumidor e idempotência. Candidaturas precisa usar os dados de Empregados e Vagas sem acessar seus bancos. A gravação da inscrição e a publicação no Kafka podem falhar em momentos diferentes. Mensagens entregues novamente não podem duplicar avaliações.

## Decisão

1. Resolver matrícula/ID numérico por HTTP e consultar os contratos de perfil/critérios por UUID, com timeout configurável. Aceitar apenas vaga ABERTA e snapshots consistentes com os IDs consultados.
2. Persistir o JSON de `CandidaturaRealizada` com os snapshots e IDs estáveis. O evento representa os dados observados no instante da consulta; não há transação distribuída que impeça uma alteração simultânea em Vagas.
3. Gravar uma outbox na mesma transação da candidatura. Um publicador confirma o envio no Kafka antes de marcar o registro como publicado. Falhas preservam a pendência para nova tentativa.
4. Usar entrega at-least-once: uma falha após o ACK e antes da marcação pode produzir duplicatas. O consumidor deve persistir uma única avaliação por candidatura, em transação local, com identificação do evento e proteção contra concorrência.
5. Calcular pontos uma vez por requisito atendido; requisito obrigatório não atendido torna a candidatura inelegível. O consumidor usa os snapshots, sem novas consultas HTTP.

## Alternativas consideradas

| Alternativa | Benefício | Custo ou motivo de descarte |
| --- | --- | --- |
| Publicar diretamente depois de gravar | Poucos componentes | Pode perder o evento se o processo cair entre as operações |
| Transação distribuída banco/Kafka | Coordenação explícita | Complexidade desproporcional ao projeto; não é necessária para entrega at-least-once |
| Consultar perfil/critérios na avaliação | Menor mensagem | Resultado dependeria de alterações posteriores, dificultando a reprodução |
| Deduplicar só em memória | Simplicidade | Perde proteção após reinício e não coordena consumidores concorrentes |

## Consequências e limites

- Há mais tabelas e um publicador de pendências, mas a publicação pode ser retomada após falhas.
- O evento mantém dados profissionais; evitar registrar payloads completos em logs e revisar retenção conforme o uso real.
- Inscrições anteriores à migração de snapshots permanecem consultáveis, sem publicação retroativa inventando dados históricos.
- Unicidade de inscrição e idempotência de consumidor são verificações diferentes.
- Pact de `ResultadoPublicado`, cache e publicação do resultado da vaga são outras entregas; não ficam concluídos por esta decisão.

## Etapas e evidências

- Etapa 1: consultas HTTP, validação e snapshot persistido. Testes de cliente HTTP e persistência em `candidaturas-service`; resultado em [fluxo-assincrono.md](../evidencias/fluxo-assincrono.md).
- Etapa 2: outbox na mesma transação da candidatura, publicador com confirmação do Kafka e retomada de pendências. O registro é bloqueado por publicação, evitando publicadores concorrentes sobre a mesma linha. A transação mantém esse bloqueio durante o envio, limitado a 12 segundos; é uma escolha simples para o volume do projeto.
- Etapa 3: cálculo e persistência idempotente da avaliação. Bloqueio pessimista da candidatura coordena entregas concorrentes; a avaliação tem PK `candidaturaId` e unicidade de `eventId`. O evento recebido deve coincidir com o snapshot original persistido, pois é interno a Candidaturas. Resultado e deduplicação são a mesma gravação transacional; falha antes do commit permite nova tentativa.
- Etapa 4: conectar o listener Kafka e comprovar o fluxo com broker, retries e mensagens inválidas (pendente).

## Regras de avaliação

Um requisito é atendido quando há pelo menos um item do mesmo `TipoItem`, com descrição contendo a palavra-chave (sem diferenciar maiúsculas/minúsculas, sem busca semântica) e meses suficientes, caso exigidos. A duração é verificada no mesmo item; não se somam experiências distintas. Horas não entram na pontuação porque o contrato atual não define horas mínimas.

Cada requisito atendido soma seus pontos uma vez, mesmo com vários itens correspondentes. Um obrigatório ausente torna a candidatura inelegível e sua pontuação final é zero. Opcionais ausentes não desclassificam. Sem requisitos, o resultado é elegível com zero pontos. Soma acima do limite inteiro é rejeitada como dado inválido, sem gravar resultado negativo.

O GET da candidatura retorna `status` PENDENTE ou AVALIADA, com `elegivel` e `pontuacao` nulos enquanto não houver resultado. A avaliação não representa publicação do resultado da vaga e ainda não emite `AvaliacaoConcluida`; esse segundo evento e ranking/cache serão tratados em outra entrega.
