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

## Etapa 3 — expiração, falhas e concorrência

Validação direcionada pela IA: `mvn -o -pl candidaturas-service -am clean verify -Dtest=Ranking*Test -Dsurefire.failIfNoSpecifiedTests=false`, BUILD SUCCESS e 13 testes aprovados:

- Dois testes de ordenação/filtragem e resposta HTTP.
- Nove testes de cache: preenchimento/hit, revisão após commit, isolamento de vagas, reentrega, TTL desde a escrita, fallback de leitura/escrita, propagação de falha da fonte, rollback após insert e preenchimento antigo atrasado (alguns cenários compartilham um teste).
- Dois testes de serialização Redis: round-trip tipado de UUID/Instant/lista, sem metadados de classe Java, e rejeição de JSON corrompido.

O TTL é verificado com relógio Caffeine controlado: leitura aos 59 segundos não renova a expiração; aos 61 segundos ocorre nova consulta. Não há espera real de um minuto no teste. A corrida usa duas threads/latches: uma lê a versão antiga, a outra confirma uma avaliação e preenche a versão nova, e só então a primeira é liberada para preencher a chave antiga. A próxima consulta continua usando o ranking novo.

Fallback é exercitado simulando falha nas operações do cache, mantendo a consulta JPA real; uma falha da fonte é propagada. O rollback é provocado depois da escrita/flush da avaliação e antes do commit, demonstrando que a revisão anterior continua válida. A verificação com Redis real será registrada separadamente.

## Build completo

Em 24/10/2026, a IA executou `mvn -o clean verify`: BUILD SUCCESS nos cinco projetos, **43 testes**, zero falhas e zero erros. São três de Empregados, dois de Vagas e 38 de Candidaturas. Os 13 novos testes de ranking/cache complementam os 30 anteriores, incluindo o fluxo Kafka.

Os testes padrão usam H2, Caffeine e Kafka KRaft embarcado, sem Docker. O teste de serialização usa o mesmo serializer JSON tipado configurado para Redis; isso não foi confundido com teste de conexão a um Redis real.

## Verificação com Redis, PostgreSQL e serviços reais

A IA iniciou os três JARs em portas HTTP livres, com PostgreSQL/Kafka locais e **Redis 7.4.11** (versão obtida por `INFO server`). Aplicou-se a migração V5 sem apagar dados existentes. A instância de verificação usou TTL de **5 segundos** para observar a expiração; o padrão da aplicação continua 60.

1. Criou-se uma vaga fictícia com Java obrigatório (10 pontos) e Spring opcional (20).
2. Um GET antes da avaliação retornou lista vazia e preencheu a chave da revisão 0.
3. A primeira candidatura, com curso Java, foi avaliada pelo Kafka com 10 pontos. O GET passou à revisão 1 e criou sua chave Redis.
4. Um GET repetido leu o JSON do Redis: a quantidade de consultas completas de ranking observada no log SQL permaneceu **2 antes / 2 depois**. A consulta simples de revisão continua ocorrendo, conforme a estratégia.
5. Outra candidata, com Java e Spring, recebeu 30 pontos. O GET passou à revisão 2, ordenando 30 antes de 10, sem depender da expiração da chave anterior.
6. A chave da revisão 2 apresentou TTL positivo de até 5 segundos, expirou e foi preenchida pela consulta seguinte.
7. Apenas a chave dessa vaga fictícia foi temporariamente substituída por JSON inválido. A aplicação respondeu com o ranking correto pelo banco e o valor original foi restaurado com expiração.
8. Uma segunda instância de Candidaturas, com mensageria desativada e conexão Redis apontada para porta indisponível, retornou o mesmo ranking pelo PostgreSQL e registrou a falha de cache. O Redis compartilhado não foi parado.

Resultados observados:

```text
APROVADO: ranking vazio na revisao 0, avaliacao muda para revisao 1, hit Redis sem repetir a consulta completa.
APROVADO: nova avaliacao invalidou logicamente o ranking; revisao 2 retorna 30 e 10 pontos.
APROVADO: TTL real de 5 segundos expirou e a consulta seguinte preencheu novamente o Redis.
APROVADO: JSON corrompido na chave exclusiva do teste retornou ranking correto pela fonte.
APROVADO: instancia com Redis apontado para porta indisponivel respondeu pelo PostgreSQL; Redis compartilhado permaneceu ativo.
redis_version:7.4.11
```

Rastreabilidade: vaga numérica `3`, UUID `6bbc888a-543e-4033-b6fc-bb3fe353784d`, matrículas fictícias `1792886338622` e `1792886338623`, candidaturas `ff1a0b0d-af9f-4ca2-9adb-f8fc5d6708c5` e `240004df-76f1-4619-a687-fd35e15c5b37`. Exemplo de chave: `selecao:ranking:v1::6bbc888a-543e-4033-b6fc-bb3fe353784d:2`.

Os processos Java criados para verificação foram encerrados. Não houve FLUSH, expurgo global, exclusão de volumes ou parada dos servidores compartilhados. Helpers e logs ficaram em `target`, ignorado pelo Git. A política de scripts do Windows foi liberada somente no processo aprovado para executar o helper local; não foi alterada permanentemente.

Limites: não é teste de carga, medição de latência nem validação de Redis Cluster/failover. A falha de conexão foi provocada por configuração de uma instância isolada, não pela interrupção do servidor compartilhado. A suíte Testcontainers e a reprodução da entrega completa a partir de clone limpo continuam pendentes.

## Roteiro para repetir a demonstração

Iniciar os serviços e a infraestrutura conforme o README. Usar uma matrícula e vaga novas no [roteiro do fluxo](fluxo-assincrono.md), com requisito Java de 10 pontos; guardar `$vaga`, `$candidatura` e `$resultado` do exemplo. O endpoint usa o UUID `$vaga.vagaId`, não o ID numérico.

```powershell
$rankingUrl = "http://localhost:8083/api/v1/candidaturas/ranking/$($vaga.vagaId)"
Invoke-RestMethod $rankingUrl
Invoke-RestMethod $rankingUrl
```

No ambiente Docker do grupo, inspecionar somente a revisão daquela vaga (com autenticação Redis se configurada):

```powershell
$revisao = docker compose exec -T postgres psql -U candidaturas_svc -d candidaturas -At -c "SELECT count(*) FROM avaliacoes WHERE vaga_id='$($vaga.vagaId)';"
$chave = "selecao:ranking:v1::$($vaga.vagaId):$($revisao.Trim())"
docker compose exec -T redis redis-cli GET $chave
docker compose exec -T redis redis-cli TTL $chave
```

Registrar outro empregado/candidatura para a mesma vaga e aguardar AVALIADA: a revisão sobe e o próximo GET preenche outra chave, com a nova classificação. Não esperar ou exigir a exclusão física imediata da chave antiga; isso não faz parte da estratégia. Uma reentrega não cria nova revisão.

Para expiração curta, iniciar somente a instância de demonstração com `--ranking.cache.ttl-segundos=5`. Sem novos GETs, observar a chave expirar; o GET seguinte preenche novamente. Para falha controlada, usar uma instância isolada com `--mensageria.enabled=false --spring.data.redis.port=1` e porta HTTP livre; ela consulta o banco sem interromper o Redis compartilhado. Se essa porta estiver em uso por Redis no ambiente, escolher outra comprovadamente indisponível.

Esses comandos Docker são um roteiro para o ambiente do grupo; a verificação desta sessão acessou Redis por protocolo TCP. Não usar `FLUSHALL`/`FLUSHDB` para demonstrar invalidação e não editar avaliações já gravadas: ambas as ações mudam o experimento e a segunda viola a premissa desta versão.
