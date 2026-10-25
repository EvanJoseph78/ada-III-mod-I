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
