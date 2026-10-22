# ADR-003 — Herança Maven e configuração comum do build

Status: aceita
Data: 2026-10-22

## Contexto

O POM raiz agrega os três serviços e `shared-contracts`. Na implementação inicial, os serviços passaram a herdar diretamente do Spring Boot, enquanto `shared-contracts` continuou herdando da raiz.

Essa configuração é válida: agregação (`modules`) e herança (`parent`) são independentes. Entretanto, os serviços não herdavam as configurações comuns da raiz, e as versões do Boot e do Java ficavam repetidas.

## Decisão

Adotar `spring-boot-starter-parent` 3.5.9 como parent da raiz e fazer os três serviços herdarem de `selecao-interna`, assim como `shared-contracts`. Definir Java 21 na raiz por `java.version`, referenciada por `maven.compiler.release`. Os serviços herdam o `groupId`; suas dependências e a ativação do plugin de empacotamento Boot permanecem nos respectivos POMs.

O objetivo é compartilhar a configuração do build, sem alterar o comportamento das APIs. Os profiles A/B/C passam a ser herdados pelos serviços, mas continuam apenas como marcadores, sem configurar os ambientes.

## Alternativas consideradas

| Alternativa | Benefícios | Motivo para não ser a escolha |
| --- | --- | --- |
| Cada serviço herdar diretamente do Boot | Administração independente do build de cada serviço | Repete configurações e não transmite as definições da raiz aos serviços |
| Parent próprio com importação do BOM do Boot | Centraliza versões de dependências sem herdar todos os padrões do Boot | Exige gerenciar explicitamente mais configurações e versões de plugins, sem necessidade identificada no escopo atual |

## Consequências e trade-offs

- Versão do Boot e configuração Java têm um ponto comum de manutenção.
- Mudanças na raiz podem afetar todos os módulos e exigem verificação do build completo.
- `shared-contracts` também herda o gerenciamento de dependências e plugins do Boot. Isso não adiciona dependências Spring nem o transforma em aplicação: permanece um JAR de contratos.
- Para construir um serviço separadamente, seu parent raiz precisa estar disponível pelo caminho local padrão `../pom.xml` ou em um repositório Maven.
- A mudança não introduz dependências entre serviços nem muda suas responsabilidades de domínio.

## Evidências

- [POM raiz](../../pom.xml).
- POMs de [Empregados](../../empregados-service/pom.xml), [Vagas](../../vagas-service/pom.xml) e [Candidaturas](../../candidaturas-service/pom.xml).
- [POM de shared-contracts](../../shared-contracts/pom.xml), que já herdava da raiz e não precisou ser alterado.
- Em 2026-10-22, a IA executou `mvn -o clean verify` na raiz, após manter somente as alterações nos POMs. Resultado: `BUILD SUCCESS` nos cinco projetos do reactor, sem Docker, com dependências previamente disponíveis no repositório Maven local.
- O resultado comprova compilação e empacotamento. Não há testes automatizados implementados na versão final deste ajuste; Pact e testes de integração continuam pendentes.
