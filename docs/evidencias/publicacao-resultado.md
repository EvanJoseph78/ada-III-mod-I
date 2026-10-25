# Publicação do resultado por etapas

## 1. Barreira de inscrições em Candidaturas

Migração V6 cria processos e a linha de coordenação transacional. A admissão verifica a barreira após obter os snapshots HTTP e mantém o bloqueio até o commit. Fechamento repetido conserva o processo; IDs divergentes retornam 409. Prontidão compara todas as inscrições com todas as avaliações, incluindo inelegíveis. Inscrição histórica não avaliada impede prontidão.

Decisão, alternativas e limites: [ADR-008](../adr/ADR-008-publicacao-resultado.md). As etapas seguintes conectam Vagas, outbox e consumidor; esta etapa isolada ainda não publica resultado.

Etapa 1 validada: quatro testes aprovados, incluindo HTTP em voo bloqueado pelo fechamento e fechamento aguardando o commit de uma inscrição já admitida. Comando: mvn -o -pl candidaturas-service -am clean test -Dtest=ProcessoVagaTest -Dsurefire.failIfNoSpecifiedTests=false.

## 2. Fechamento coordenado por Vagas

POST /api/v1/vagas/{vagaId}/encerramento chama a barreira local de Candidaturas e avança ABERTA para EM_AVALIACAO. Repetição é permitida nesse estado; falha remota preserva o estado anterior e permite recuperação por repetição. Exclusão de vaga fora de ABERTA retorna 409, coordenada pelo mesmo bloqueio do estado. Timeouts HTTP configuráveis; sem consulta direta a outro banco.

Onze testes direcionados aprovados: dois de fechamento, dois de persistência e sete interações Pact. Publicação Kafka entra na próxima etapa.

## 3. Outbox de resultados em Vagas

Migração V2, produtor String/JSON com chave vagaId e dispatcher implementados. Três testes aprovados: confirmação somente após ACK, falha/retomada preservando payload e participação na transação chamadora. MENSAGERIA_ENABLED=false desliga o dispatcher/criação do tópico, sem deixar de persistir intenções. A conexão com o comando de publicação entra na etapa 4.

## 4. Comandos de publicação e conclusão

POST /api/v1/vagas/{vagaId}/resultado exige EM_AVALIACAO e prontidão remota, grava status/outbox na mesma transação e responde 202. Repetição/conclusão prematura/transição inválida retornam 409. POST /api/v1/vagas/{vagaId}/conclusao avança RESULTADO_PUBLICADO para CONCLUIDA. Quatro testes aprovados, incluindo publicação concorrente com apenas uma outbox e rollback do status quando o registro da intenção falha. Conclusão é administrativa: não significa confirmação de consumo remoto.

## 5. Resultado definitivo em Candidaturas

Consumidor de vagas.resultados e GET /api/v1/candidaturas/resultados/{vagaId} implementados. O consumidor bloqueia o processo fechado, confere prontidão e grava snapshot do ranking diretamente do banco. Reentrega idêntica preserva evento/data/resultado; divergência é rejeitada. Quatro testes aprovados: concorrência/replay, avaliação pendente com retomada, rollback/resultado vazio e eventos inválidos. A leitura definitiva não usa Redis nem altera o contrato do ranking parcial em cache. Integração com broker e fluxo entre serviços será verificada a seguir.

## 6. Kafka, falhas HTTP e build completo

A suíte completa passou com 76 testes, zero falhas/erros/ignorados, em mvn -o -PB clean verify. FluxoKafkaTest tem seis cenários: quatro anteriores e dois novos cobrindo consumo/reentrega de ResultadoPublicado, preservação do snapshot, divergência/JSON inválido para vagas.resultados.DLT e retry de falha transitória. A espera pelo consumo tolera 404 até o resultado existir, dentro do prazo do teste.

Três testes HTTP do cliente de coordenação verificam vínculo dos IDs, prontidão, contagens divergentes, campos ausentes, JSON inválido, 404/500 e timeout. Respostas incompletas nunca liberam publicação. O contrato Pact existente continua passando; os endpoints novos ainda não têm Pact próprio.
