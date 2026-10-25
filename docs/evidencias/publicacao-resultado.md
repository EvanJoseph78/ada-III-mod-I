# Publicação do resultado por etapas

## 1. Barreira de inscrições em Candidaturas

Migração V6 cria processos e a linha de coordenação transacional. A admissão verifica a barreira após obter os snapshots HTTP e mantém o bloqueio até o commit. Fechamento repetido conserva o processo; IDs divergentes retornam 409. Prontidão compara todas as inscrições com todas as avaliações, incluindo inelegíveis. Inscrição histórica não avaliada impede prontidão.

Decisão, alternativas e limites: [ADR-008](../adr/ADR-008-publicacao-resultado.md). As etapas seguintes conectam Vagas, outbox e consumidor; esta etapa isolada ainda não publica resultado.

Etapa 1 validada: quatro testes aprovados, incluindo HTTP em voo bloqueado pelo fechamento e fechamento aguardando o commit de uma inscrição já admitida. Comando: mvn -o -pl candidaturas-service -am clean test -Dtest=ProcessoVagaTest -Dsurefire.failIfNoSpecifiedTests=false.
