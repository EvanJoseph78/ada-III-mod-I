# Verificação final da entrega — 25/10/2026

## Requisitos técnicos CORE

| Requisito | Evidência |
| --- | --- |
| Pelo menos dois serviços e bases segregadas | Três serviços/bancos/usuários; [persistência](persistencia.md), [infraestrutura](execucao-infra.md) e ADR-004 |
| Fluxo assíncrono com produtor/consumidor idempotente | [Inscrição/avaliação](fluxo-assincrono.md) e [publicação](publicacao-resultado.md), com outbox, replay, concorrência, retry e DLT |
| Cache com estratégia/invalidação explícitas | [Ranking Redis](cache-ranking.md), TTL, revisão, fallback e ADR-006 |
| Contrato executável entre dois serviços | [Pact HTTP](contrato-http-vagas.md), JSON versionado e verificação contra API real |
| ADRs e README de arquitetura | [ADRs 001–008](../adr/README.md), [arquitetura](../arquitetura.md) e [README](../../README.md) |
| Execução em perfil declarado | 76 testes sem Docker no perfil B e três JARs com dependências locais, inclusive a partir do clone descrito abaixo |

O checklist técnico CORE está implementado e comprovado no recorte declarado. Isso não atribui nota nem comprova revisão humana, contribuição individual ou ensaio da banca.

## Clone local limpo

Foi criado um clone independente do commit `8276454` da branch `feat/fluxo-core`:

```powershell
git clone --no-hardlinks --single-branch . target/validacao-clone-core
Set-Location target/validacao-clone-core
mvn -o -B -PB clean verify
```

Resultado: **BUILD SUCCESS, 76 testes**, zero falhas/erros/ignorados: Empregados 3, Vagas 21, Candidaturas 52. Foram compilados novos artefatos nessa cópia, sem reutilizar os diretórios `target` da pasta original. O repositório Maven local já tinha as dependências; isso não é uma prova de download em máquina sem cache. Primeira execução com acesso à rede: `mvn -PB clean verify`.

O clone foi local, não um download novo do GitHub. `.env` e notas pessoais ignoradas não foram copiados. A verificação final de `git status --short` no clone não mostrou alterações versionadas. Os commits posteriores a essa revisão apenas registram estas evidências e atualizam referências documentais, sem alterar o código validado.

## Execução com os JARs do clone

O helper `scripts/validar-fluxo.ps1` do próprio clone foi executado contra PostgreSQL/Kafka/Redis locais. As credenciais foram herdadas no ambiente do processo, sem criar `.env` no clone ou imprimi-las. A infraestrutura existente não foi recriada.

Novamente passaram fechamento/bloqueio de inscrição tardia, publicação rejeitada com avaliações pendentes, avaliação Kafka, ranking/cache 30/10, publicação 202 com dispatcher pausado, retomada após reiniciar Vagas, resultado preservado após reiniciar Candidaturas e conclusão administrativa. Dados fictícios dessa repetição: vaga numérica `5`, UUID `0375f9d3-8fd5-4eb4-944a-cd86bf78d70f`, evento `39d2d13d-e8bb-4e40-848a-ba9f68837568`, status final CONCLUIDA.

Logs/resultados ficaram em diretórios `target`, ignorados pelo Git. Somente processos criados pelo helper foram encerrados; bancos, dados, volumes e infraestrutura foram preservados. Limites e instruções detalhadas em [publicação](publicacao-resultado.md).

## Preparação humana antes da banca

- Revisar implementação, decisões, recorte e autoavaliação com o grupo; registrar papéis reais de André, Evandro e Rafael.
- Publicar os commits e combinar a integração da branch. Esta execução não fez push ou merge.
- Ensaiar os 12 minutos, com uma pessoa operando a demo, falas distribuídas e gravação de apoio identificada como tal.
- Deixar serviços/infraestrutura prontos, dependências baixadas, credenciais configuradas e dados fictícios preparados antes da apresentação.
- Manter as aplicações normais executando no dia; o helper de verificação encerra as aplicações que criou.

Não foram implementados Testcontainers, frontend/autenticação, reabertura/retificação, Pact dos novos endpoints/eventos, múltiplos consumidores de negócio ou alta disponibilidade. São limites/evoluções do recorte, não requisitos CORE adicionais. A visão inicial de produto é mais ampla que a entrega; ver [escopo](../escopo.md#recorte-entregue).
