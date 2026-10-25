# ADR-007 — Contrato executável HTTP entre Candidaturas e Vagas

Status: aceita e implementada
Data: 2026-10-24

## Contexto

O CORE pede um contrato executável entre dois serviços. A ADR-002 planejava cobrir a mensagem `ResultadoPublicado`, ainda sem fluxo implementado. A integração HTTP de inscrição já usa o serviço Vagas: resolve o ID numérico para UUID e consulta os critérios que serão congelados no snapshot. Uma alteração incompatível nesses endpoints pode impedir novas inscrições.

## Decisão

- Priorizar o Pact HTTP da integração existente, substituindo a escolha inicial de mensagem da ADR-002 para atender ao primeiro contrato obrigatório. A publicação do resultado continua uma entrega funcional posterior; não afirmar que ela foi implementada pelo contrato HTTP.
- Consumidor: `candidaturas-service`, exercitando `DadosCandidaturaClient` real contra o servidor mock do Pact. Empregados é um stub separado e não faz parte deste contrato.
- Provedor: `vagas-service`, verificando o arquivo versionado contra a aplicação Spring Boot real, com HTTP em porta aleatória, controller, serviço, JPA e migrações H2. Não substituir o controller/serviço do provedor por mocks.
- Gerar o JSON em `candidaturas-service/target/pacts`; revisar e versionar em `shared-contracts/pacts`. A verificação do provedor usa a cópia versionada, independente da ordem dos módulos Maven.
- Após gerar, comparar automaticamente com o JSON versionado, ignorando somente metadados da ferramenta. A regeneração intencional exige propriedade explícita, revisão/cópia e build normal; não permitir que uma execução padrão aceite silenciosamente contrato desatualizado.
- Cobrir resolução por ID, critérios de vaga aberta/em avaliação, referência inexistente e critérios inexistentes. Os estados do provedor preparam dados fictícios isolados; não dependem de PostgreSQL, Kafka ou Redis externos.
- Pact JVM 4.6.17, somente em escopo de teste, com versão centralizada. Pact Broker não é necessário; nenhum resultado é publicado externamente.

## Alternativas e custos

| Alternativa | Avaliação |
| --- | --- |
| Esperar implementar `ResultadoPublicado` | Mantém o plano inicial, mas adia a proteção de uma integração já usada e acopla o CORE a outra funcionalidade |
| Apenas compartilhar records Java | Não verifica rota, código HTTP, serialização nem compatibilidade entre implementações |
| Apenas testes HTTP com respostas escritas à mão | Testam o consumidor, mas não comprovam que Vagas fornece o que foi simulado |
| Pact Broker | Ajuda com versões e implantação independente, mas adiciona infraestrutura desnecessária ao escopo local |

O contrato verifica as expectativas de Candidaturas, não toda a API de Vagas, nem toda a semântica do negócio. Empregados e eventos continuam sem Pact. H2 não substitui a evidência PostgreSQL. Arquivos gerados devem acompanhar mudanças de expectativas; testar só o provedor com um arquivo antigo não basta.

## Referências

- [Pact consumidor JUnit 5](https://docs.pact.io/implementation_guides/jvm/consumer/junit5).
- [Pact provedor JUnit 5](https://docs.pact.io/implementation_guides/jvm/provider/junit5).
- [ADR-002](ADR-002-estrategia-de-testes.md).

## Evidências

Implementação e comandos em [contrato HTTP](../evidencias/contrato-http-vagas.md). Geração (quatro testes de consumidor), verificação (sete interações do provedor), comparação de artefato e experimento de incompatibilidade aprovados em 24/10/2026. Build completo: 54 testes sem Docker.
