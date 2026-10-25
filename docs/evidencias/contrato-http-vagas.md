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

## Etapa 3: sincronização e prova de incompatibilidade

O callback `ContratoVersionado` termina após a extensão Pact gravar o JSON. Compara estruturalmente o arquivo gerado nesta execução com a cópia versionada, ignorando somente metadados da ferramenta. Falta do arquivo ou diferença nas interações faz o build falhar. A geração intencional usa `-Dpact.atualizar=true`, revisão/cópia do arquivo e depois build normal sem essa propriedade; procedimento em [pacts/README](../../shared-contracts/pacts/README.md).

Experimento controlado em 24/10/2026: a cópia versionada teve temporariamente `titulo` renomeado para `tituloIncompativel`. O provedor falhou em duas interações, informando a ausência do campo na API real. Separadamente, os quatro cenários do consumidor executaram e a comparação final falhou por divergência do JSON. Ambos os comandos retornaram código 1. O arquivo original foi restaurado em `finally` antes do build completo; nenhuma alteração experimental foi mantida.

```sh
mvn -o clean verify
```

Resultado final: **BUILD SUCCESS, 54 testes**, zero falhas, erros ou testes ignorados. Distribuição: Empregados 3; Vagas 9 (2 existentes + 7 interações Pact); Candidaturas 42 (38 existentes + 4 Pact). A comparação do arquivo ocorre no callback final, sem acrescentar um caso ao total quando passa. Dependências foram baixadas previamente; uma primeira execução deve usar Maven com acesso aos repositórios.

Uma execução incremental carregou classes de teste inconsistentes geradas pelo editor (`Unresolved compilation problem`); a compilação limpa corrigiu o artefato. O resultado de entrega acima veio de `clean verify`, compilando tudo pelo Maven.

## Limites

- Pact verifica as expectativas deste consumidor: duas rotas e estados selecionados. Não é cobertura de toda a API, teste de carga ou prova de transação distribuída.
- Empregados é stub no consumidor; não possui contrato Pact nesta etapa.
- H2 é a substituição explícita do PostgreSQL nesta verificação; Kafka e Redis não são necessários ao contrato HTTP.
- Publicação de resultado, transições de status e evento `ResultadoPublicado` continuam sem implementação. O primeiro requisito de contrato está atendido pelo par HTTP, conforme ADR-007.
- Sem Pact Broker, a cópia versionada é o artefato de troca; a comparação automática detecta desatualização neste repositório, mas não controla versões implantadas independentemente.

Verificação da etapa 1 em 24/10/2026: quatro testes aprovados, zero falhas/erros. JSON gerado pelo Pact e copiado para o diretório versionado. A verificação do provedor ainda é a próxima etapa.
