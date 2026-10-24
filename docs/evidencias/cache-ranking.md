# Ranking e cache — acompanhamento por etapa

Data: 2026-10-24.

## Etapa 1 — consulta de ranking

Implementado: `GET /api/v1/candidaturas/ranking/{vagaId}`, usando UUID da vaga. A consulta filtra avaliações elegíveis, separa as vagas e ordena pontos/data de inscrição/UUID. Retorna posições sequenciais e uma lista vazia se não houver resultados locais; não garante existência da vaga nem publicação final.

`RankingConsultaTest` verifica ordenação, desempate por data, exclusão de inelegíveis/outra vaga e resposta HTTP (incluindo lista vazia e UUID inválido). A migração V5 cria índice para a consulta.

Validação pela IA: `mvn -o -pl candidaturas-service -am clean verify -Dtest=RankingConsultaTest -Dsurefire.failIfNoSpecifiedTests=false`, com BUILD SUCCESS e dois testes aprovados. Esta primeira etapa ainda consulta diretamente o banco; Redis e invalidação pertencem às etapas seguintes.
