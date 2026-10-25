# ADR-008 — Fechamento de inscrições e publicação durável

Status: aceita; implementação em etapas
Data: 2026-10-24

## Decisão

Vagas é responsável pelo estado do processo: ABERTA → EM_AVALIACAO → RESULTADO_PUBLICADO → CONCLUIDA. Fechar inscrições é uma operação explícita antes da publicação. Vagas solicita primeiro uma barreira persistente em Candidaturas, onde as inscrições são efetivamente gravadas. Novas inscrições consultam o estado remoto e, antes do INSERT, verificam essa barreira na mesma transação da candidatura/outbox. Assim, uma consulta HTTP iniciada antes do fechamento não permite gravação depois dele.

Para evitar a corrida na criação da primeira barreira por vaga, uma linha sentinela, criada pela migração, coordena admissões e fechamentos com bloqueio pessimista. A consulta HTTP ocorre antes desse bloqueio. A solução serializa brevemente as admissões de todas as vagas; é um custo deliberado para este escopo. Em maior volume, substituir por coordenação por vaga com criação concorrente segura. Não é um lock apenas em memória.

Fechamento é idempotente e sem reabertura automática. Se a chamada fechar em Candidaturas e a resposta/commit de Vagas falhar, repetir o fechamento recupera o processo. Nesse intervalo Vagas pode ainda indicar ABERTA, mas Candidaturas rejeita novas inscrições. Isso preserva segurança com disponibilidade reduzida, sem prometer transação distribuída.

A publicação exige inscrições fechadas e todas avaliadas. Inscrições históricas sem snapshot/avaliação impedem publicar e precisam de tratamento explícito, não são descartadas silenciosamente. Zero inscritos permite resultado vazio.

Vagas grava a transição e `ResultadoPublicado` em sua própria outbox atomicamente. O dispatcher aguarda ACK antes de marcar envio; falha mantém pendência e a janela ACK/commit admite reenvio (at-least-once). Candidaturas recebe o evento, confere prontidão e persiste uma cópia definitiva do ranking com identificação/data originais do evento. Reentrega idêntica não altera o resultado; publicação divergente é rejeitada. Dados inválidos ou falhas esgotadas seguem para DLT com operação manual.

O ranking parcial/cache continua separado da consulta definitiva: publicação não adiciona campos mutáveis ao JSON em cache nem muda sua revisão. A cópia definitiva é persistida diretamente do banco e não depende de Redis. RESULTADO_PUBLICADO em Vagas significa intenção de publicação durável; até o consumo concluir, o resultado definitivo em Candidaturas pode ainda não estar disponível.

## Alternativas

- Publicar diretamente de ABERTA: arrisca inscrições em voo e avaliações faltantes.
- Verificar apenas o status remoto: deixa uma janela entre HTTP e commit local.
- Publicar Kafka antes/depois do commit sem outbox: deixa janela de perda da intenção ou mensagem sem estado correspondente.
- Atualizar bancos dos dois serviços diretamente: viola propriedade dos dados.
- SAGA com reabertura/compensação: aumenta o escopo; fechamento monotônico e repetível permite recuperação explícita sem aceitar inscrições tardias.

## Limites

Não há autenticação/autorização; endpoints de coordenação são internos por convenção, sem isolamento de rede implementado. As APIs assumem IDs de Vagas válidos enviados por esse serviço; não oferecer como comandos públicos para usuários finais. Não há retificação/segunda publicação, expurgo automático ou alta disponibilidade local. Exclusões de vagas que avançaram de ABERTA serão rejeitadas. Evoluir segurança, revisão de resultados e retenção antes de uso real.

Evidências por etapa em [publicação](../evidencias/publicacao-resultado.md).
