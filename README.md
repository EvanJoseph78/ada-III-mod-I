# Seleção Interna e Movimentação de Empregados

Projeto final BE-JV-010 — Nível III.

## Escopo

Perfis profissionais, vagas, inscrições, avaliação curricular, pontuação e ranking. Ver [escopo](docs/escopo.md).

## Módulos

- empregados-service: perfil, cursos, certificações e experiências.
- vagas-service: vagas, requisitos, critérios e publicação de resultados.
- candidaturas-service: inscrições, avaliação e ranking.
- shared-contracts: contratos de integração, sem entidades de persistência compartilhadas.

Cada serviço usa sua própria base PostgreSQL e seu próprio usuário. Kafka está previsto para os eventos de candidatura, avaliação e resultado.

## Ambiente e execução

Perfil alvo: **A — Docker em máquina pessoal**.

Estado atual: aplicações Spring Boot 3.5.9 com Java 21, persistência JPA e migrações Flyway para os três bancos PostgreSQL. Há testes de integração com H2 sem Docker. O fluxo entre serviços, Kafka, cache, avaliação e ranking ainda não foi implementado. Os profiles Maven A/B/C continuam como marcadores e não configuram os ambientes.

### Build e testes sem Docker

Pré-requisitos: JDK 21 e Maven. Na raiz:

```sh
mvn clean verify
```

O reactor compila primeiro `shared-contracts`, executa os testes e gera os JARs dos serviços. Os testes usam o profile Spring `test`, ativado pelas classes de teste, com H2 em modo PostgreSQL e as mesmas migrações Flyway da aplicação. O profile Spring `test` não é o profile Maven B. O resultado e seus limites estão em [persistencia.md](docs/evidencias/persistencia.md).

### Serviços com PostgreSQL

Com Docker disponível, iniciar o banco sem apagar volumes:

```sh
docker compose up -d --wait postgres
```

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

### Kafka local

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
