# ADR-006 — Ranking com cache-aside, revisão por vaga e fallback

Status: aceita; implementação em etapas
Data: 2026-10-24

## Contexto

O CORE exige um cache com estratégia e invalidação explícitas. A avaliação já persiste resultados imutáveis por candidatura. Consultas frequentes do ranking repetiriam a junção e ordenação desses resultados. Redis está previsto no ambiente; a suíte sem Docker usa Caffeine, conforme ADR-002.

## Decisão

- Expor ranking parcial por UUID da vaga, apenas com candidaturas já avaliadas e elegíveis. Ordenar por pontos decrescentes, data de inscrição crescente e UUID da candidatura crescente como desempate técnico determinístico. A posição é sequencial, sem posições compartilhadas.
- Usar cache-aside: consultar cache, carregar do banco em falta e preencher com TTL de 60 segundos, configurável. Redis é o backend da aplicação; Caffeine substitui Redis na suíte sem Docker.
- A chave inclui vaga e revisão. A revisão é o total de avaliações confirmadas para aquela vaga, incluindo inelegíveis. Como avaliações são apenas inseridas e ficam imutáveis, uma avaliação nova muda a revisão no mesmo commit em que se torna visível ao ranking. Uma reentrega não muda a revisão.
- Ler a revisão no banco a cada GET, para não depender de um evento de invalidação separado. O cache evita a consulta completa de classificação, não todas as consultas ao banco.
- A invalidação é lógica: uma revisão nova usa outra chave, e entradas antigas expiram pelo TTL. Uma leitura antiga que termina tarde só preenche a chave antiga, sem sobrescrever a atual. Não há varredura/DEL global.
- Em erro de leitura, gravação ou desserialização do cache, consultar a fonte e retornar o ranking; não usar memória local silenciosamente na aplicação. Falha do banco não é escondida pelo cache, pois a revisão depende da fonte autoritativa.

## Alternativas consideradas

| Alternativa | Benefício | Motivo para não ser a escolha |
| --- | --- | --- |
| Apenas TTL, sem revisão | Menos consultas e implementação simples | Pode devolver uma classificação antiga por todo o TTL após uma avaliação |
| Cache-aside com chave fixa e eviction após commit | Estratégia conhecida, atualiza após alterações | Uma consulta em andamento pode repor um valor antigo depois da remoção; exigiria coordenação adicional |
| Write-through | Mantém cache no caminho de escrita | Acopla a avaliação ao Redis e exige recompor a classificação no momento de cada escrita |
| Revisão persistida em tabela dedicada | Suporta alterações/exclusões futuras sem depender de contagem | Introduz coordenação e mais escrita por vaga; desnecessária enquanto avaliações forem imutáveis e somente inseridas |

## Consequências e limites

- Cada GET ainda faz uma contagem indexada por vaga. Não se afirma ganho de latência ou redução percentual sem medição; a redução verificada é da consulta completa de ranking em cache hit.
- A contagem não é um contador O(1); uma tabela de revisão/paginação pode ser necessária em volume maior.
- Não introduzir edição/exclusão de avaliações sem rever a estratégia: reutilizar uma revisão pode tornar uma entrada antiga válida indevidamente. Limpar/recriar dados também exige limpar o namespace do cache correspondente.
- O ranking pode estar incompleto enquanto existirem avaliações pendentes e não representa resultado publicado da seleção.
- Sem avaliação elegível, retornar lista vazia (inclusive para UUID de vaga desconhecida localmente); o endpoint não consulta Vagas para provar existência.
- Não armazenar snapshots profissionais completos no cache: a resposta contém posição, IDs, matrícula, pontos e data de inscrição.

## Etapas e evidências

1. Consulta e endpoint de ranking, índice e testes de ordenação/filtragem.
2. Configuração Redis/Caffeine, cache-aside com chave por revisão e invalidação lógica.
3. Testes de TTL, concorrência, rollback/fallback e verificação com Redis local.

Resultado e limites em [cache-ranking.md](../evidencias/cache-ranking.md).
