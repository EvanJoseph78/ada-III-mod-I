# Arquitetura — estado atual e evolução

Atualizada em 2026-10-24. Três serviços com bases e credenciais próprias; entidades JPA não são compartilhadas. O fluxo de inscrição e avaliação está implementado. Ranking/cache e publicação do resultado da vaga ainda são pendentes.

| Contexto | Responsabilidade | Dados próprios |
| --- | --- | --- |
| Empregados | Perfil e qualificações | Empregados e itens de formação/experiência |
| Vagas | Oportunidades e critérios | Vagas, status e requisitos |
| Candidaturas | Inscrição e avaliação | Candidaturas, snapshots, outbox e avaliações |

## Fluxo implementado

```mermaid
sequenceDiagram
    participant U as Cliente
    participant C as Candidaturas
    participant E as Empregados
    participant V as Vagas
    participant DB as Banco de Candidaturas
    participant K as Kafka
    U->>C: POST /api/v1/candidaturas
    C->>E: Resolver matrícula e consultar perfil
    C->>V: Resolver ID e consultar critérios
    C->>DB: Transação: candidatura + snapshots + outbox
    C-->>U: 201 (registro durável, avaliação pendente)
    C->>DB: Buscar pendências da outbox
    C->>K: CandidaturaRealizada (chave candidaturaId)
    K-->>C: Confirmação do envio
    C->>DB: Marcar publicação confirmada
    K->>C: Entrega ao grupo avaliacao-curricular
    C->>DB: Bloquear candidatura e consultar avaliação
    C->>DB: Se inédita, gravar resultado e identidade do evento
    C-->>K: Retorno do listener; confirmação do offset
    U->>C: GET /api/v1/candidaturas/{id}
    C-->>U: PENDENTE ou AVALIADA, elegibilidade e pontuação
```

Cada consulta HTTP possui timeout configurável. Referência inexistente ou vaga não aberta impede o registro. Os snapshots preservam o que foi observado na consulta, mas não bloqueiam alterações simultâneas no outro serviço; não há transação distribuída de inscrição.

Outbox e candidatura são gravadas atomicamente no PostgreSQL de Candidaturas. O dispatcher aguarda o ACK do Kafka e só então marca a publicação. Se o processo falhar entre ACK e commit, o evento pode ser reenviado: a garantia é **at-least-once**. A idempotência do produtor Kafka não elimina essa janela entre sistemas.

O consumidor usa apenas o snapshot original. Bloqueio pessimista da candidatura e resultado único por candidatura/evento protegem contra reentrega e concorrência; a gravação da avaliação é transacional. O evento é interno ao serviço e deve coincidir com o payload persistido. As regras de cálculo estão na [ADR-005](adr/ADR-005-fluxo-assincrono.md).

## Falhas e recuperação

- Envio Kafka que falha mantém a outbox pendente e interrompe o lote; um próximo ciclo tenta novamente.
- O listener não confirma processamento antes da transação de avaliação terminar. Falhas transitórias têm até duas novas tentativas, com backoff exponencial (padrão 1 e 2 segundos).
- JSON inválido ou evento inconsistente segue para `candidaturas.realizadas.DLT` sem retry de negócio. Falhas persistentes seguem para a mesma DLT após três tentativas totais. Se a publicação na DLT falhar, o erro não é tratado como recuperação bem-sucedida.
- A DLT preserva payload e metadados para investigação; não há consumidor nem replay automático dela. Após corrigir a causa, reenviar o mesmo evento com a mesma chave/IDs, sem fabricar um novo snapshot. A avaliação fica PENDENTE enquanto não houver resultado persistido, inclusive se houver uma mensagem na DLT.
- Inscrições históricas sem snapshots não são publicadas retroativamente. A retenção/limpeza da outbox e da DLT deve preservar pendências e rastreabilidade; não há expurgo automático nesta versão.

## Infraestrutura e testes

O perfil alvo A usa Kafka, PostgreSQL e Redis no Compose; os serviços executam na JVM. A outbox e as avaliações usam o banco exclusivo de Candidaturas. Os tópicos de candidatura e DLT têm três partições e fator de replicação 1, adequado somente ao nó local de desenvolvimento.

A suíte padrão usa H2 e Kafka KRaft embarcado, sem Docker. Testes verificam consultas HTTP, snapshots, atomicidade da outbox, cálculo, reentrega, concorrência, retry e DLT. Evidências e comandos em [fluxo-assincrono.md](evidencias/fluxo-assincrono.md). Testes com H2/broker embarcado e validação com a infraestrutura real são evidências distintas.

## Entregas ainda pendentes

- Ranking, cache com TTL/invalidação/fallback e testes.
- Publicação do resultado da vaga e evento `ResultadoPublicado` com Pact entre Vagas e Candidaturas.
- Evento `AvaliacaoConcluida` e seus consumidores de ranking/cache, se mantidos no desenho final; ainda não são emitidos.
- Revisão da política de exclusão de vaga, operações de correção/replay e retenção de eventos.
- Reprodução a partir de clone limpo e demonstração de todos os requisitos obrigatórios.
