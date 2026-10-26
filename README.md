# Seleção Interna e Movimentação de Empregados

Projeto final BE-JV-010 — Nível III.

## Escopo

Perfis profissionais, vagas, inscrições, avaliação curricular, pontuação e ranking. Ver [escopo](docs/escopo.md).

## Módulos

- empregados-service: perfil, cursos, certificações e experiências.
- vagas-service: vagas, requisitos, critérios e publicação de resultados.
- candidaturas-service: inscrições, avaliação e ranking.
- shared-contracts: contratos de integração, sem entidades de persistência compartilhadas.

Cada serviço usa sua própria base PostgreSQL e seu próprio usuário. Kafka transporta candidaturas para avaliação e resultados publicados de Vagas para Candidaturas. `AvaliacaoConcluida` permanece apenas definido; o ranking atual não depende desse evento.

## Ambiente e execução

Perfil alvo: **A — Docker em máquina pessoal**.

Estado atual: inscrição com HTTP/snapshots/outbox, avaliação Kafka idempotente, ranking com cache-aside Redis, Pact HTTP entre serviços, fechamento de inscrições, publicação durável e resultado definitivo implementados. Build aprovado com **76 testes** no perfil B, sem Docker. Execução dos três JARs com infraestrutura local comprovou o fluxo até CONCLUIDA, retomada de publicação e persistência após reinício. Ver [evidências](docs/evidencias/publicacao-resultado.md) e [recorte entregue](docs/escopo.md#recorte-entregue).

### Build e testes sem Docker

Pré-requisitos: JDK 21 e Maven. Na raiz:

```sh
mvn clean verify
```

O reactor compila primeiro `shared-contracts`, executa os testes e gera os JARs dos serviços. Os testes usam o profile Spring `test`, ativado pelas classes de teste, com H2 em modo PostgreSQL e as mesmas migrações Flyway da aplicação. O profile Spring `test` não é o profile Maven B. O resultado e seus limites estão em [persistencia.md](docs/evidencias/persistencia.md).

`FluxoKafkaTest` inicia um broker KRaft na JVM, em portas livres, para verificar publicação/consumo, reentrega, retry e DLT. Não precisa do Kafka local nem de Docker. O teste usa cliente/broker 3.9.1, gerenciados pelas dependências; a infraestrutura do Compose declara Kafka 4.1.1. Evidências e limites em [fluxo-assincrono.md](docs/evidencias/fluxo-assincrono.md).

Padrão e `mvn -PB clean verify` excluem a etiqueta JUnit `docker`; `-PA` remove a exclusão para testes dessa etiqueta quando existirem. Testcontainers ainda não foi implementado, portanto A não acrescenta testes de contêineres hoje. Os perfis Maven não sobem o Compose nem substituem variáveis das aplicações; C identifica a alternativa conceitual. Para modo offline, usar `mvn -o -PB clean verify` após baixar dependências. A execução real usa serviços JVM e dependências locais, com Compose como infraestrutura de referência (ADR-001).

### Contrato executável entre serviços

O Pact cobre as consultas de vaga/critérios usadas por Candidaturas. O consumidor usa `DadosCandidaturaClient` real; o provedor verifica o [JSON versionado](shared-contracts/pacts/candidaturas-service-vagas-service.json) contra Vagas com HTTP real em porta aleatória, controller/JPA e H2. São quatro testes de consumidor e sete interações verificadas pelo provedor, executados pelo build padrão sem Docker ou Pact Broker.

A geração escreve em `target/pacts` e a execução normal compara com a cópia versionada. Para alterar expectativas, seguir o [procedimento de atualização](shared-contracts/pacts/README.md). [ADR-007](docs/adr/ADR-007-contrato-http-vagas.md) e [evidências](docs/evidencias/contrato-http-vagas.md) explicam o escopo: este contrato atende ao CORE, mas não implementa publicação do resultado nem verifica `ResultadoPublicado`.

### Serviços com PostgreSQL

Com Docker disponível, iniciar o banco sem apagar volumes:

```sh
docker compose up -d --wait postgres
```

Para a avaliação assíncrona, iniciar também Kafka com `docker compose up -d --wait kafka` antes de Candidaturas. Para o cache do ranking, iniciar Redis com `docker compose up -d --wait redis`; na indisponibilidade a consulta usa o banco.

O Compose cria os bancos e usuários na primeira inicialização do volume. Os padrões locais são:

| Serviço | Porta HTTP | Banco | Usuário | Senha local padrão |
| --- | --- | --- | --- | --- |
| Empregados | 8081 | empregados | empregados_svc | empregados-local |
| Vagas | 8082 | vagas | vagas_svc | vagas-local |
| Candidaturas | 8083 | candidaturas | candidaturas_svc | candidaturas-local |

Cada aplicação aplica suas migrações e valida o schema ao iniciar. Não há acesso direto ao banco de outro serviço. Em três terminais separados, após o build:

```sh
java -jar empregados-service/target/empregados-service-0.1.0-SNAPSHOT.jar
java -jar vagas-service/target/vagas-service-0.1.0-SNAPSHOT.jar
java -jar candidaturas-service/target/candidaturas-service-0.1.0-SNAPSHOT.jar
```

O arquivo `.env` é lido pelo Compose, mas não automaticamente pelas aplicações Java. Caso as senhas do banco sejam diferentes dos padrões, exportar a variável correspondente no terminal de cada serviço antes de iniciar. Exemplo PowerShell:

```powershell
$env:EMPREGADOS_DB_PASSWORD = 'senha-configurada-no-banco'
```

As variáveis aceitas seguem o padrão `EMPREGADOS_DB_URL`, `EMPREGADOS_DB_USER` e `EMPREGADOS_DB_PASSWORD`; para os outros serviços, substituir o prefixo por `VAGAS` ou `CANDIDATURAS`. A URL padrão é `jdbc:postgresql://localhost:5432/<banco>`. `SERVER_PORT` altera a porta HTTP do serviço naquele terminal. Alterar a senha no `.env` não modifica usuários de um volume já inicializado; usar as credenciais existentes ou alterá-las explicitamente no banco.

O roteiro inicial está em [persistencia.md](docs/evidencias/persistencia.md). A validação mais recente do fluxo completo, com publicação/reinícios, está em [publicacao-resultado.md](docs/evidencias/publicacao-resultado.md).

### Fechamento e publicação do resultado

Depois das inscrições, usar UUID da vaga nas rotas:

```text
POST /api/v1/vagas/{vagaId}/encerramento
POST /api/v1/vagas/{vagaId}/resultado
GET  /api/v1/candidaturas/resultados/{vagaId}
POST /api/v1/vagas/{vagaId}/conclusao
```

O fechamento responde 200 e avança ABERTA → EM_AVALIACAO, criando antes uma barreira em Candidaturas. É repetível nesse estado, inclusive para recuperar falha de resposta/commit entre serviços. Novas inscrições retornam 409 após a barreira, mesmo que o HTTP tenha começado antes. Configurar em Vagas `CANDIDATURAS_SERVICE_URL` (padrão `http://localhost:8083`) e `INTEGRACOES_TIMEOUT_MS` (2000).

A publicação exige todas as inscrições avaliadas e grava RESULTADO_PUBLICADO/outbox atomicamente, respondendo **202**. A entrega Kafka é posterior. Consultar o resultado definitivo até 200; 404 significa que ainda não está disponível ou não existe resultado local. Repetir publicação, publicar antes de fechar ou com avaliações pendentes retorna 409. Zero inscritos permite resultado vazio. Inscrições históricas não avaliadas também bloqueiam publicação e exigem tratamento explícito.

Vagas usa `KAFKA_BOOTSTRAP_SERVERS` e `OUTBOX_INTERVALO_MS` com os mesmos padrões de Candidaturas. `MENSAGERIA_ENABLED=false` em Vagas pausa seu dispatcher/criação de tópico, preservando outbox; em Candidaturas pausa dispatcher e listeners. O consumidor de `vagas.resultados` usa `RESULTADO_CONSUMER_GROUP` (padrão `resultados-publicados`), com retry/DLT e gravação idempotente. Divergências não sobrescrevem o resultado. DLT: `vagas.resultados.DLT`, replay manual com evento original após corrigir a causa.

O resultado definitivo contém publicação original, ranking persistido e contagens; não depende de Redis. O ranking parcial/cache fica separado. CONCLUIDA é conclusão administrativa após RESULTADO_PUBLICADO, não ACK do consumidor. Exclusão fora de ABERTA retorna 409. Não há reabertura/segunda publicação. [ADR-008](docs/adr/ADR-008-publicacao-resultado.md).

### Demonstração reproduzível no Windows

Para chamadas manuais, abrir [requisicoes.http](requisicoes.http) na extensão REST Client do VS Code. O arquivo reúne todos os endpoints implementados, cadastros fictícios e referências aos IDs retornados. Seguir a sequência indicada no cabeçalho e aguardar as avaliações/consumo antes de avançar.

No Postman, importar a [coleção dos três serviços](postman/Selecao-Interna.postman_collection.json), com captura automática de IDs. [Ordem de execução e instruções](postman/README.md).

Com infraestrutura pronta, portas 18081/18082/18083 livres e JARs construídos:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/validar-fluxo.ps1
```

O helper inicia aplicações próprias, cria dados fictícios, testa fechamento/avaliação/ranking/publicação, reinicia serviços e confere persistência/conclusão. Carrega explicitamente senhas do `.env` se ausentes no ambiente, sem imprimi-las. A política de scripts é liberada apenas nesse processo. Logs/IDs ficam em `target/validacao-fluxo`. Encerra suas aplicações e mantém infraestrutura/dados. Não executar o ensaio de pausa junto com outras instâncias processando os mesmos bancos/grupos. Na banca, iniciar os três serviços normais e mantê-los disponíveis; usar o helper para ensaiar/gravar previamente.

### Cache do ranking

`GET /api/v1/candidaturas/ranking/{vagaId}` consulta o ranking parcial de elegíveis, por UUID da vaga. Redis é usado como cache-aside, com `REDIS_HOST` (localhost), `REDIS_PORT` (6379), `REDIS_PASSWORD` (vazio por padrão) e `RANKING_CACHE_TTL_SEGUNDOS` (60). Os timeouts de conexão/comando são 300 ms. Configurar as mesmas credenciais do ambiente; as aplicações Java não carregam o `.env` automaticamente.

A chave inclui a revisão (total de avaliações da vaga): um novo resultado confirmado invalida logicamente a entrada anterior, que expira pelo TTL. A revisão é consultada no banco em cada GET, mesmo no cache hit. Em falha de Redis, o ranking é lido do banco; não há fallback silencioso para cache local na aplicação. `RANKING_CACHE_TIPO=caffeine` é uma substituição explícita usada nos testes sem Docker, não uma topologia de cache distribuído. Ver [ADR-006](docs/adr/ADR-006-cache-ranking.md) e [evidências](docs/evidencias/cache-ranking.md).

Na validação real com Redis 7.4.11 foram comprovados hit sem repetir a consulta completa, chave nova após avaliação, expiração e fallback por falha de conexão/JSON inválido. O TTL padrão é desde a escrita, não é renovado por leituras. Avaliações são imutáveis; edição/exclusão/restauração de dados exige rever a revisão e limpar o namespace correspondente. Se a resposta passar a incluir outros dados mutáveis, eles também deverão participar da revisão. Este ranking parcial não é o resultado publicado da vaga.

### Kafka e avaliação

A inscrição já consulta Empregados e Vagas por HTTP antes de gravar seus snapshots. Configurar em Candidaturas `EMPREGADOS_SERVICE_URL` e `VAGAS_SERVICE_URL` (padrões `http://localhost:8081` e `http://localhost:8082`, sem `/api/v1`) e `INTEGRACOES_TIMEOUT_MS` (padrão 2000 por conexão/leitura). Consultar [contratos](docs/contratos.md) para os códigos de erro e [acompanhamento do fluxo](docs/evidencias/fluxo-assincrono.md) para a etapa atual.

Candidaturas grava uma outbox junto com a inscrição e publica pendências a cada `OUTBOX_INTERVALO_MS` (padrão 1000). Configurar `KAFKA_BOOTSTRAP_SERVERS` (padrão `localhost:9092`). `MENSAGERIA_ENABLED=false` desativa dispatcher, listener e criação dos tópicos para execução somente HTTP/persistência; a outbox continua sendo gravada para envio posterior. A resposta 201 confirma o registro durável, não a conclusão da avaliação.

Consultar `GET /api/v1/candidaturas/{candidaturaId}` até `status=AVALIADA` para obter `elegivel` e `pontuacao`. O grupo é configurável por `AVALIACAO_CONSUMER_GROUP` (padrão `avaliacao-curricular`). Reentregas preservam resultado/data. Falhas transitórias usam duas novas tentativas com backoff exponencial, iniciado por `AVALIACAO_RETRY_INTERVALO_MS` (padrão 1000). Falhas persistentes ou dados inválidos seguem para `candidaturas.realizadas.DLT`; não há replay automático. Uma inscrição sem resultado continua PENDENTE, por isso investigar também a DLT em caso de espera prolongada. O [roteiro do fluxo](docs/evidencias/fluxo-assincrono.md) inclui demonstração e repetição de eventos.

Com Docker em execução, na raiz do repositório:

```sh
docker compose config --quiet
docker compose up -d --wait kafka
docker compose ps
```

O primeiro comando valida a configuração; o segundo baixa a imagem quando necessário e aguarda o healthcheck. Para diagnosticar falhas:

Antes do broker, o serviço auxiliar `kafka-init` ajusta o proprietário do volume para UID/GID `1000:1000`, usuário da imagem Kafka. Somente essa preparação executa como root; o broker mantém o usuário padrão. O volume é preservado. O Compose aguarda a conclusão bem-sucedida dessa preparação antes de iniciar o Kafka.

```sh
docker compose logs --tail 100 kafka
```

Aplicações executadas na máquina usam `localhost:9092`; contêineres na mesma rede Compose usam `kafka:19092`. A configuração usa a imagem oficial `apache/kafka:4.1.1`, um único nó KRaft e comunicação sem autenticação, destinada ao desenvolvimento local. Não há tolerância à perda do nó. Referência: [exemplo oficial do Apache Kafka](https://github.com/apache/kafka/blob/4.1.1/docker/examples/docker-compose-files/single-node/plaintext/docker-compose.yml).

Para encerrar preservando os dados no volume:

```sh
docker compose down
```

Com Maven instalado, validar a estrutura na raiz:

```sh
mvn validate
```

Esse comando valida apenas a estrutura. Para compilar, empacotar e executar os testes implementados, usar `mvn clean verify`.

## Documentação

[Verificação final](docs/evidencias/entrega-final.md): clone local limpo com 76 testes e execução do fluxo com os JARs desse clone. O checklist técnico CORE está atendido no recorte declarado; revisão humana, papéis e ensaio da banca continuam sob responsabilidade do grupo.

- [Arquitetura](docs/arquitetura.md)
- [ADRs](docs/adr/README.md)
- [Autoavaliação](AVALIACAO.md)
- [Uso crítico de IA](docs/uso-de-ia.md)
- [Enunciado](docs/referencia/projeto-final-brief-alunos.md)

Integrantes: André, Evandro e Rafael. Com três integrantes, opcionais não são obrigatórios. Papéis efetivamente exercidos e revisão humana final devem ser conferidos pelo grupo antes da entrega; não foram atribuídos automaticamente.
