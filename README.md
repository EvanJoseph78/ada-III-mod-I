# Seleção Interna e Movimentação de Empregados

Projeto final BE-JV-010 — Nível III.

## Escopo

Perfis profissionais, vagas, inscrições, avaliação curricular, pontuação e ranking. Ver [escopo](docs/escopo.md).

## Módulos

- empregados-service: perfil, cursos, certificações e experiências.
- vagas-service: vagas, requisitos, critérios e publicação de resultados.
- candidaturas-service: inscrições, avaliação e ranking.
- shared-contracts: contratos de integração, sem entidades de persistência compartilhadas.

Cada serviço usa sua própria base PostgreSQL e seu próprio usuário. Kafka transporta candidaturas para avaliação; eventos de avaliação concluída e publicação do resultado da vaga continuam planejados.

## Ambiente e execução

Perfil alvo: **A — Docker em máquina pessoal**.

Estado atual: aplicações Spring Boot 3.5.9 com Java 21, persistência JPA e migrações Flyway nos três bancos. A inscrição consulta perfil/critérios por HTTP, grava snapshots e outbox e é avaliada por consumidor Kafka idempotente, com retry/DLT. Há 30 testes sem Docker (H2 e Kafka KRaft embarcado) e validação do fluxo com infraestrutura local. Ranking parcial e cache-aside estão implementados e em validação; publicação do resultado da vaga e Pact continuam pendentes. Os profiles Maven A/B/C continuam como marcadores e não configuram os ambientes.

### Build e testes sem Docker

Pré-requisitos: JDK 21 e Maven. Na raiz:

```sh
mvn clean verify
```

O reactor compila primeiro `shared-contracts`, executa os testes e gera os JARs dos serviços. Os testes usam o profile Spring `test`, ativado pelas classes de teste, com H2 em modo PostgreSQL e as mesmas migrações Flyway da aplicação. O profile Spring `test` não é o profile Maven B. O resultado e seus limites estão em [persistencia.md](docs/evidencias/persistencia.md).

`FluxoKafkaTest` inicia um broker KRaft na JVM, em portas livres, para verificar publicação/consumo, reentrega, retry e DLT. Não precisa do Kafka local nem de Docker. O teste usa cliente/broker 3.9.1, gerenciados pelas dependências; a infraestrutura do Compose declara Kafka 4.1.1. Evidências e limites em [fluxo-assincrono.md](docs/evidencias/fluxo-assincrono.md).

### Serviços com PostgreSQL

Com Docker disponível, iniciar o banco sem apagar volumes:

```sh
docker compose up -d --wait postgres
```

Para a avaliação assíncrona, iniciar também Kafka com `docker compose up -d --wait kafka` antes de Candidaturas. Redis ainda não é utilizado pelo fluxo implementado.

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

O roteiro de cadastro, consulta e verificação após reiniciar está em [persistencia.md](docs/evidencias/persistencia.md). A integração foi validada com sete testes H2 e com PostgreSQL 17.11, incluindo leitura dos dados após reiniciar os três serviços. A execução a partir de um clone limpo e o fluxo completo entre serviços continuam pendentes.

### Cache do ranking

`GET /api/v1/candidaturas/ranking/{vagaId}` consulta o ranking parcial de elegíveis, por UUID da vaga. Redis é usado como cache-aside, com `REDIS_HOST` (localhost), `REDIS_PORT` (6379), `REDIS_PASSWORD` (vazio por padrão) e `RANKING_CACHE_TTL_SEGUNDOS` (60). Os timeouts de conexão/comando são 300 ms. Configurar as mesmas credenciais do ambiente; as aplicações Java não carregam o `.env` automaticamente.

A chave inclui a revisão (total de avaliações da vaga): um novo resultado confirmado invalida logicamente a entrada anterior, que expira pelo TTL. A revisão é consultada no banco em cada GET, mesmo no cache hit. Em falha de Redis, o ranking é lido do banco; não há fallback silencioso para cache local na aplicação. `RANKING_CACHE_TIPO=caffeine` é uma substituição explícita usada nos testes sem Docker, não uma topologia de cache distribuído. Ver [ADR-006](docs/adr/ADR-006-cache-ranking.md) e [evidências](docs/evidencias/cache-ranking.md).

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

- [Arquitetura](docs/arquitetura.md)
- [ADRs](docs/adr/README.md)
- [Autoavaliação](AVALIACAO.md)
- [Uso crítico de IA](docs/uso-de-ia.md)
- [Enunciado](docs/referencia/projeto-final-brief-alunos.md)

Integrantes, papéis e opcionais: pendentes de registro.
