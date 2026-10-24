# Ranking e cache — acompanhamento por etapa

Data: 2026-10-24.

## Etapa 1 — consulta de ranking

Implementado: `GET /api/v1/candidaturas/ranking/{vagaId}`, usando UUID da vaga. A consulta filtra avaliações elegíveis, separa as vagas e ordena pontos/data de inscrição/UUID. Retorna posições sequenciais e uma lista vazia se não houver resultados locais; não garante existência da vaga nem publicação final.

`RankingConsultaTest` verifica ordenação, desempate por data, exclusão de inelegíveis/outra vaga e resposta HTTP (incluindo lista vazia e UUID inválido). A migração V5 cria índice para a consulta.

Validação pela IA: `mvn -o -pl candidaturas-service -am clean verify -Dtest=RankingConsultaTest -Dsurefire.failIfNoSpecifiedTests=false`, com BUILD SUCCESS e dois testes aprovados. Esta primeira etapa ainda consulta diretamente o banco; Redis e invalidação pertencem às etapas seguintes.

## Etapa 2 — cache-aside e invalidação lógica

Implementado: Redis na aplicação e Caffeine na suíte sem Docker, TTL padrão 60 segundos e chave `selecao:ranking:v1::<vagaId>:<revisao>`. Revisão é a contagem de avaliações confirmadas daquela vaga, incluindo inelegíveis. Cada GET lê essa revisão no banco antes de acessar o cache. O cache guarda a resposta tipada do ranking em JSON, sem serialização nativa Java ou entidades JPA.

Nova avaliação invalida logicamente a chave anterior ao mudar a revisão no mesmo commit do resultado. Entradas antigas não são removidas imediatamente: deixam de ser utilizadas e expiram pelo TTL. Esse mecanismo depende de avaliações imutáveis e somente inseridas; não é write-through. Uma candidatura apenas registrada, sem avaliação, não altera a classificação.

`RankingCacheTest` usa Caffeine real e consulta JPA real para verificar preenchimento/hit, chave nova após avaliação, isolamento entre vagas e reentrega que não altera a revisão. O spy conta as consultas completas; a consulta de revisão continua ocorrendo em cada GET. Não há medição de ganho percentual de latência nesta etapa.

O código já possui fallback para erro de cache, mas testes de falha/TTL/concorrência e a execução com Redis real pertencem à etapa 3.

Validação pela IA: `mvn -o -pl candidaturas-service -am clean verify -Dtest=RankingConsultaTest,RankingCacheTest -Dsurefire.failIfNoSpecifiedTests=false`, BUILD SUCCESS e seis testes aprovados (dois de ranking e quatro de cache/invalidação).
