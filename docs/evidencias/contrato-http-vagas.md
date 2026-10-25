# Contrato HTTP — Candidaturas consome Vagas

## Etapa 1: expectativas do consumidor

`VagasContractConsumerTest`, em Candidaturas, usa o `DadosCandidaturaClient` da aplicação contra o mock HTTP do Pact. Empregados é um stub auxiliar e não integra o par verificado.

As expectativas incluem os campos usados na resolução de ID e nos critérios (UUID, título, status e requisitos), além de códigos 404 convertidos em 422 e vaga não aberta convertida em 409. Os dois GETs são interações independentes; o estado de uma vaga pode mudar entre consultas. O contrato não promete uma transação distribuída.

```sh
mvn -pl candidaturas-service -am test -Dtest=VagasContractConsumerTest -Dsurefire.failIfNoSpecifiedTests=false
```

O Pact escreve `candidaturas-service/target/pacts/candidaturas-service-vagas-service.json`. A cópia revisada será versionada em `shared-contracts/pacts`; o provedor verificará essa cópia na etapa 2. Até a verificação do provedor passar, não considerar o requisito CORE concluído.

Decisão e limites na [ADR-007](../adr/ADR-007-contrato-http-vagas.md).

## Etapa 2: verificação do provedor

`VagasContractProviderTest` lê o JSON versionado e executa sete interações contra o servidor Spring Boot em porta aleatória. Controller, serviço e repositório reais usam um banco H2 exclusivo (`vagas-pact`), criado pelas migrações Flyway. Cada interação prepara seu estado; a execução não depende de ordem ou de serviços externos. As fixtures SQL ficam somente nesse banco de teste e não alteram a aplicação.

```sh
mvn -o -pl vagas-service -am test -Dtest=VagasContractProviderTest -Dsurefire.failIfNoSpecifiedTests=false
```

Em 24/10/2026: sete verificações aprovadas, zero falhas/erros. A etapa do consumidor teve quatro testes aprovados. O provedor foi executado isoladamente, sem executar Candidaturas antes, comprovando que usa a cópia versionada.

Na primeira verificação, dois GETs de critérios não tinham estado próprio no Pact gerado e retornaram 404. O teste detectou o problema; as expectativas foram corrigidas para declarar o estado em cada interação, o JSON foi regenerado e as sete verificações passaram. Isso evita dependência acidental da ordem das interações.

Verificação da etapa 1 em 24/10/2026: quatro testes aprovados, zero falhas/erros. JSON gerado pelo Pact e copiado para o diretório versionado. A verificação do provedor ainda é a próxima etapa.
