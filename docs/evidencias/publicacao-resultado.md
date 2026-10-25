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

## 7. Verificação com PostgreSQL, Kafka e Redis locais

O helper [scripts/validar-fluxo.ps1](../../scripts/validar-fluxo.ps1) iniciou os três JARs em portas exclusivas, aplicando V2 de Vagas e V6 de Candidaturas sem apagar dados. Usou as credenciais do ambiente/`.env`, sem imprimi-las. A infraestrutura existente permaneceu ativa; essa execução não comprova recriação de contêineres.

Foram verificados:

1. Duas inscrições gravadas com mensageria pausada; publicação antes de fechar retornou 409.
2. Fechamento em Vagas/Candidaturas; terceira inscrição com outro empregado rejeitada com 409 e publicação pendente de avaliação também rejeitada.
3. Reinício de Candidaturas com mensageria ativa: outbox retomada e duas avaliações concluídas. Ranking parcial: Bia 30, Ana 10; consulta repetida.
4. Publicação retornou 202 com dispatcher de Vagas desativado; resultado definitivo ainda retornou 404.
5. Reinício de Vagas com mensageria ativa entregou a intenção persistida. Resultado definitivo trouxe duas inscrições/avaliações e ranking 30/10.
6. Reinício de Candidaturas preservou exatamente o JSON do resultado (incluindo eventId, data original e classificação).
7. Nova publicação rejeitada com 409; conclusão administrativa retornou CONCLUIDA.

Dados fictícios para rastreabilidade: vaga numérica `4`, UUID `2672628e-61cd-4c25-92a9-d576c0456f7f`; matrículas `1792896535742` e `1792896535743`; candidaturas `a956fd56-e290-4f3f-a33d-ea8915db2491` e `9d74ae74-415d-47fa-81ee-d7e9c72a5c4e`; evento de publicação `ad827c7d-722f-4351-85e6-b729849759c2`. Ocorrido em `2026-10-25T02:49:09.628596900Z` (noite de 24/10, horário de São Paulo).

Saída final:

```text
APROVADO: fechamento bloqueia inscricao tardia e publicacao com avaliacao pendente.
APROVADO: ranking 30/10; publicacao aceita e pendente com dispatcher de Vagas desativado.
APROVADO: publicacao retomada, resultado persistido apos reinicio e processo CONCLUIDA.
Processos criados pelo helper encerrados; infraestrutura e dados preservados.
```

Logs e JSON ficaram em `target/validacao-fluxo`, ignorado pelo Git. Somente os processos Java criados pelo helper foram encerrados; sem exclusão de bancos/volumes, parada da infraestrutura ou limpeza global de Redis. A liberação da política de scripts foi restrita ao processo PowerShell usado na verificação.

### Repetir

Com PostgreSQL/Kafka/Redis disponíveis, credenciais corretas e sem outras instâncias processando os mesmos bancos/grupos durante os cenários de pausa:

```powershell
mvn -PB clean verify
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validar-fluxo.ps1
```

O helper usa por padrão portas 18081/18082/18083 livres; podem ser alteradas pelos parâmetros EmpregadosPort/VagasPort/CandidaturasPort. Ele carrega explicitamente senhas do `.env` somente se ainda não estiverem no ambiente do processo. Isso é comportamento do helper, não das aplicações Java. Não executar simultaneamente com outro ensaio de pausa: os bancos/grupos são compartilhados. O helper deixa dados fictícios para inspeção e finaliza suas aplicações; na banca, iniciar as aplicações normais conforme README para mantê-las disponíveis.

### Limites

Não é teste de carga, alta disponibilidade, segurança ou retificação de resultado. Não foi derrubado o broker compartilhado: falhas de envio são testadas na suíte, e a execução real demonstrou pausa explícita de mensageria e retomada após reinício. Não há confirmação distribuída de consumo no status CONCLUIDA; consultar o resultado definitivo separadamente. Inscrições antigas sem avaliação exigem tratamento antes de publicar a vaga correspondente.
