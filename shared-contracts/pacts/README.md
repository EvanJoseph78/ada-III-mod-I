# Pact HTTP de Candidaturas para Vagas

`candidaturas-service-vagas-service.json` é gerado por `VagasContractConsumerTest`, usando o cliente real de Candidaturas, e verificado por `VagasContractProviderTest` contra a API real de Vagas com H2. Não editar o JSON à mão.

## Execução normal

Na raiz, `mvn verify` executa os dois lados sem Docker. `mvn -o verify` funciona após baixar as dependências. A verificação de Vagas usa este arquivo, independentemente da ordem dos módulos. Ao terminar, o teste de Candidaturas compara o contrato gerado nesta execução com a cópia versionada e falha se houver divergência (exceto metadados da ferramenta).

## Mudança intencional do contrato

1. Alterar as expectativas do consumidor e a implementação necessária do provedor.
2. Gerar explicitamente, na raiz:

```sh
mvn -pl candidaturas-service -am test -Dtest=VagasContractConsumerTest -Dsurefire.failIfNoSpecifiedTests=false -Dpact.atualizar=true
```

3. Revisar o JSON gerado em `candidaturas-service/target/pacts/candidaturas-service-vagas-service.json` e copiar para este diretório. No PowerShell:

```powershell
Copy-Item -LiteralPath candidaturas-service/target/pacts/candidaturas-service-vagas-service.json -Destination shared-contracts/pacts/candidaturas-service-vagas-service.json
```

4. Executar `mvn verify` sem `pact.atualizar`. Versionar expectativas, implementação e JSON juntos. Essa propriedade permite regenerar para revisão; não aprova compatibilidade e não deve ser usada no build de entrega.

Não há Pact Broker, Docker ou publicação externa. Este é um contrato HTTP; não cobre `ResultadoPublicado` nem a API de Empregados. Decisão na [ADR-007](../../docs/adr/ADR-007-contrato-http-vagas.md) e evidências em [contrato-http-vagas.md](../../docs/evidencias/contrato-http-vagas.md).
