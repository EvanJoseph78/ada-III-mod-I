# Fluxo assíncrono — acompanhamento por etapa

Data: 2026-10-24.

## Etapa 1 — integração HTTP e snapshots

Implementado: resolução dos IDs numéricos para UUID, consulta de perfil/critérios, validação de identidade e status ABERTA e persistência do evento com snapshots na mesma linha da candidatura. A migração V2 preserva inscrições antigas sem snapshot.

Testes: `DadosCandidaturaClientTest` usa um servidor HTTP local na JVM para verificar sucesso, referência ausente, vaga fechada, falha remota, timeout e identidade inconsistente. `CandidaturasPersistenceTest` verifica snapshots persistidos, duplicata e ausência de gravação após falhas nas consultas.

Validação executada pela IA: `mvn -o -pl candidaturas-service -am clean verify`, com `BUILD SUCCESS` e nove testes de Candidaturas aprovados (seis HTTP e três de persistência). A execução valida o módulo e suas dependências sem Docker; não é um teste contra os serviços reais nem um Pact.

Ainda não há envio ao broker nesta etapa. A existência do snapshot não conclui produtor/consumidor/idempotência.

## Etapa 2 — outbox e publicador Kafka

Implementado: migração V3, gravação de candidatura/outbox na mesma transação e publicação de até 20 pendências por ciclo, com chave `candidaturaId`, payload persistido e tópico `candidaturas.realizadas`. A marcação ocorre após confirmação do Kafka. Falhas mantêm o evento pendente para nova tentativa; o dispatcher interrompe o lote na primeira falha para não multiplicar esperas durante indisponibilidade.

O produtor usa strings JSON, `acks=all` e idempotência do cliente Kafka; isso não elimina a possibilidade de reenvio após falha entre ACK e marcação no banco. Inscrições históricas não são preenchidas nem publicadas retroativamente. A outbox não deve ser limpa enquanto houver eventos pendentes.

`OutboxPublicacaoTest` usa H2 e simula a confirmação/falha do Kafka para verificar marcação após ACK, reenvio com payload estável e rollback da inscrição quando a escrita da outbox falha. Esses testes não comprovam comunicação com um broker; essa verificação pertence à etapa 3.

Validação executada pela IA: `mvn -o -pl candidaturas-service -am clean verify`, `BUILD SUCCESS`, 12 testes de Candidaturas aprovados (nove da etapa 1 e três de outbox).

## Etapa 3 — cálculo e idempotência persistente

Implementado: migração V4 e avaliação única por candidatura, com unicidade de evento, conferência do snapshot original e bloqueio pessimista. O GET da inscrição passa a expor status, elegibilidade e pontuação. As regras estão na ADR-005.

`AvaliadorCurricularTest` verifica múltiplos itens para o mesmo requisito, obrigatórios/opcionais, duração mínima e overflow. `AvaliacaoIdempotenciaTest` verifica reentrega sem repetir cálculo ou alterar a data, duas transações concorrentes, rejeição de evento divergente e recuperação após falha transitória. Ainda não é um teste de reentrega pelo Kafka: o listener e os testes com broker vêm na etapa 4.

Validação executada pela IA: `mvn -o -pl candidaturas-service -am clean verify`, `BUILD SUCCESS`, 21 testes de Candidaturas aprovados (12 anteriores, cinco de regras e quatro de idempotência).

## Etapa 4 — listener, retry/DLT e fluxo completo

`FluxoKafkaTest` usa Kafka KRaft embarcado e H2, sem Docker ou repositórios Kafka simulados. As consultas de dados remotos são substituídas por fixture nesse teste; o cliente HTTP é exercitado separadamente pelo servidor HTTP local da etapa 1.

Quatro cenários passaram:

1. A outbox publica, o listener grava uma avaliação e a reentrega do mesmo payload não repete cálculo nem altera a data.
2. Uma falha transitória recebe retry e conclui com um único resultado.
3. JSON inválido chega à DLT sem executar cálculo.
4. Uma falha que persiste esgota três tentativas, segue para DLT e não grava avaliação parcial.

Validação completa executada pela IA: **`mvn -o clean verify`**, `BUILD SUCCESS` nos cinco projetos, **30 testes**, zero falhas e zero erros:

| Conjunto | Testes |
| --- | ---: |
| Empregados / persistência e currículo | 3 |
| Vagas / persistência e critérios | 2 |
| Candidaturas / integração HTTP | 6 |
| Candidaturas / inscrição e snapshots | 3 |
| Candidaturas / outbox | 3 |
| Candidaturas / regras de avaliação | 5 |
| Candidaturas / idempotência e concorrência | 4 |
| Candidaturas / Kafka, retry e DLT | 4 |

Versões: Spring Boot 3.5.9, Spring Kafka 3.3.11, cliente e Kafka embarcado 3.9.1. O Compose define a imagem `apache/kafka:4.1.1`; o broker local também foi exercitado abaixo, mas sua versão binária não foi consultada diretamente. A suíte padrão não requer Docker; a suíte Testcontainers continua pendente.

## Verificação com serviços e infraestrutura locais

Em 24/10/2026, a IA iniciou os três JARs em portas HTTP livres, configurou as URLs reais de Empregados/Vagas em Candidaturas e usou PostgreSQL 17.11 em `localhost:5432` e Kafka em `localhost:9092`, já disponíveis. As migrações V2/V3/V4 foram aplicadas sem apagar as tabelas ou os registros anteriores.

Foi cadastrado empregado fictício com curso Java, vaga com requisito obrigatório de 10 pontos e inscrição. O POST real executou as consultas HTTP, gravou snapshots/outbox e retornou 201. O GET passou a AVALIADA, com elegibilidade verdadeira e 10 pontos; repetir a inscrição retornou 409.

O mesmo payload persistido foi reenviado ao Kafka com a mesma chave. Um helper local de verificação aguardou o offset confirmado do grupo ultrapassar o offset da mensagem e conferiu no PostgreSQL: uma única avaliação, 10 pontos e data original preservada. Depois de encerrar as instâncias anteriores, uma nova instância de Candidaturas recuperou o resultado e repetiu essa verificação de reentrega com sucesso. Isso comprova que a deduplicação não depende da memória do processo.

```text
APROVADO: HTTP real -> snapshots/outbox PostgreSQL -> Kafka local -> AVALIADA, elegivel=true, pontuacao=10; duplicata HTTP=409.
APROVADO: reentrega confirmada pelo offset Kafka; uma avaliacao, 10 pontos e data original preservada.
APROVADO: instancia anterior encerrada; nova instancia recuperou resultado e confirmou reentrega sem duplicar nem alterar a data.
```

Rastreabilidade dos dados fictícios preservados: matrícula `1792882337601`, empregado `1944705c-3131-4afb-8d1f-8ee8988a8983`, vaga numérica `2` / UUID `bb74146e-bb18-4031-b796-5df7b86cbbeb`, candidatura `88fb2d1d-23f1-4d93-9241-77b1bbd30c44`. Os processos Java de verificação foram encerrados; banco, broker e dados foram preservados. Helpers e logs ficaram em `target`, ignorado pelo Git.

Limites: o teste local não reiniciou o banco nem o broker, não simulou queda de energia entre ACK e commit, nem reproduziu um clone limpo. Falhas transitórias, DLT e atomicidade estão cobertas pelos testes automatizados descritos acima. Cache, ranking, Pact e publicação do resultado da vaga não foram apresentados como concluídos.

## Roteiro para repetir a demonstração

Iniciar infraestrutura e os três JARs conforme o README, com credenciais iguais às dos bancos. O trecho abaixo usa as portas padrão e uma matrícula nova:

```powershell
$matricula = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$perfil = @{ matriculaEmpregado = $matricula; nomeEmpregado = 'Teste fluxo'; itens = @(@{ tipo = 'CURSO'; descricao = 'Java'; horas = 40 }) } | ConvertTo-Json -Depth 4
$empregado = Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/v1/empregados -ContentType application/json -Body $perfil
$criterios = @{ nomeVaga = 'Teste Java'; requisitos = @(@{ tipo = 'CURSO'; palavraChave = 'Java'; obrigatorio = $true; pontos = 10 }) } | ConvertTo-Json -Depth 4
$vaga = Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/v1/vagas -ContentType application/json -Body $criterios
$inscricao = @{ matriculaEmpregado = $matricula; idVaga = $vaga.idVaga } | ConvertTo-Json
$candidatura = Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/v1/candidaturas -ContentType application/json -Body $inscricao
for ($tentativa = 0; $tentativa -lt 30; $tentativa++) {
    $resultado = Invoke-RestMethod "http://localhost:8083/api/v1/candidaturas/$($candidatura.candidaturaId)"
    if ($resultado.status -eq 'AVALIADA') { break }
    Start-Sleep -Seconds 1
}
if ($resultado.status -ne 'AVALIADA' -or $resultado.pontuacao -ne 10) { throw 'Avaliacao nao concluiu como esperado' }
$resultado
```

Para repetir o evento no ambiente Docker, consultar o payload original e publicá-lo com a mesma chave. Não inventar outro eventId ou trocar os snapshots:

```powershell
$payload = (docker compose exec -T postgres psql -U candidaturas_svc -d candidaturas -At -c "SELECT evento_candidatura FROM candidaturas WHERE candidatura_id='$($candidatura.candidaturaId)';") -join ''
"$($candidatura.candidaturaId)|$payload" | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:19092 --topic candidaturas.realizadas --property parse.key=true --property 'key.separator=|'
docker compose exec -T postgres psql -U candidaturas_svc -d candidaturas -c "SELECT candidatura_id,event_id,elegivel,pontuacao,avaliada_em FROM avaliacoes WHERE candidatura_id='$($candidatura.candidaturaId)';"
```

Comparar a avaliação antes e depois do consumo; ID, pontos e data devem permanecer. O trecho Docker é um roteiro para o ambiente do grupo, não foi executado via CLI nesta sessão. Para comprovação automatizada sem dependências externas, executar `mvn clean verify`: os testes Kafka também aguardam o consumo/reentrega antes das asserções.

Em espera prolongada, verificar outbox pendente, logs por eventId e o tópico DLT. Falhas permanentes não desaparecem sozinhas: corrigir a causa antes do replay manual. Não há consumidor automático da DLT nem mudança automática do status PENDENTE para um status de falha.
