# Seleção Interna e Movimentação de Empregados

Projeto final BE-JV-010 — Nível III.

## Escopo

Perfis profissionais, vagas, inscrições, avaliação curricular, pontuação e ranking. Ver [escopo](docs/escopo.md).

## Módulos

- empregados-service: perfil, cursos, certificações e experiências.
- vagas-service: vagas, requisitos, critérios e publicação de resultados.
- candidaturas-service: inscrições, avaliação e ranking.
- shared-contracts: contratos de integração, sem entidades de persistência compartilhadas.

Cada serviço terá sua própria base. Kafka está previsto para os eventos de candidatura, avaliação e resultado.

## Ambiente e execução

Perfil alvo: **A — Docker em máquina pessoal**.

Estado atual: estrutura inicial e Compose para Kafka local; execução ainda a validar. Não há aplicações executáveis. Os profiles Maven A/B/C são marcadores e ainda não configuram os ambientes. JDK, framework, banco e cache estão pendentes.

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

Esse comando não comprova funcionalidades. As instruções para executar serviços e testes serão acrescentadas com a implementação. A rubrica exige também testes sem Docker, mesmo com o perfil A como alvo.

## Documentação

- [Arquitetura](docs/arquitetura.md)
- [ADRs](docs/adr/README.md)
- [Autoavaliação](AVALIACAO.md)
- [Uso crítico de IA](docs/uso-de-ia.md)
- [Enunciado](docs/referencia/projeto-final-brief-alunos.md)

Integrantes, papéis e opcionais: pendentes de registro.
