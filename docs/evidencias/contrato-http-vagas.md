# Contrato HTTP — Candidaturas consome Vagas

## Etapa 1: expectativas do consumidor

`VagasContractConsumerTest`, em Candidaturas, usa o `DadosCandidaturaClient` da aplicação contra o mock HTTP do Pact. Empregados é um stub auxiliar e não integra o par verificado.

As expectativas incluem os campos usados na resolução de ID e nos critérios (UUID, título, status e requisitos), além de códigos 404 convertidos em 422 e vaga não aberta convertida em 409. Os dois GETs são interações independentes; o estado de uma vaga pode mudar entre consultas. O contrato não promete uma transação distribuída.

```sh
mvn -pl candidaturas-service -am test -Dtest=VagasContractConsumerTest -Dsurefire.failIfNoSpecifiedTests=false
```

O Pact escreve `candidaturas-service/target/pacts/candidaturas-service-vagas-service.json`. A cópia revisada será versionada em `shared-contracts/pacts`; o provedor verificará essa cópia na etapa 2. Até a verificação do provedor passar, não considerar o requisito CORE concluído.

Decisão e limites na [ADR-007](../adr/ADR-007-contrato-http-vagas.md).

Verificação da etapa 1 em 24/10/2026: quatro testes aprovados, zero falhas/erros. JSON gerado pelo Pact e copiado para o diretório versionado. A verificação do provedor ainda é a próxima etapa.
