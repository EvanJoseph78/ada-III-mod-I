# Uso crítico de IA

## 2026-10-24 — Cache, etapa 3

A IA adicionou testes de expiração com relógio controlado, preenchimento antigo atrasado, rollback após escrita e fallback sem ocultar falha da fonte. A serialização JSON tipada também foi verificada. O build completo aprovou 43 testes sem Docker.

Na verificação real, a IA usou Redis 7.4.11, PostgreSQL e os três serviços: confirmou cache hit sem repetir a consulta completa, novas revisões após avaliações e expiração com TTL reduzido a 5 segundos apenas na instância de teste. JSON inválido foi injetado/restaurado somente na chave de uma vaga fictícia; uma instância isolada com porta Redis indisponível confirmou fallback sem parar o servidor compartilhado. Não houve FLUSH ou exclusão de volumes. O bloqueio de execução de scripts do Windows exigiu aprovação para liberar somente o processo do helper local, sem mudança permanente de política.

Decisões e limitações estão na ADR-006: ainda há contagem indexada no banco em cada GET; a revisão depende de resultados imutáveis e não foi alegado ganho percentual de latência. O grupo deve revisar a regra de desempate e a futura publicação de resultados; Pact e clone limpo continuam pendentes.

## 2026-10-24 — Cache, etapa 2

A IA implementou cache-aside com Redis, JSON tipado, TTL e substituição explícita por Caffeine nos testes. A invalidação é lógica por revisão (contagem de avaliações confirmadas por vaga), evitando repovoamento de uma chave nova por uma leitura antiga. Seis testes de ranking/cache foram aprovados. A documentação registra o custo da consulta de revisão em cada GET e a restrição a avaliações imutáveis; não foi alegada redução percentual de latência. Falhas, concorrência, TTL e Redis real seguem na próxima etapa.

## 2026-10-24 — Cache, etapa 1

O responsável solicitou concluir cache com estratégia/invalidação explícitas e commits separados. A IA começou pela fonte de dados: ranking parcial de avaliações elegíveis, ordenação determinística e endpoint HTTP. Dois testes H2/HTTP foram aprovados em compilação limpa. A ADR-006 distingue a consulta pronta da camada Redis ainda pendente e registra a escolha de cache-aside com revisão por vaga, condicionada a avaliações imutáveis e somente inseridas.

## 2026-10-24 — Fluxo assíncrono, etapa 4

Conforme a solicitação de concluir produtor/consumidor/idempotência em etapas, a IA conectou o listener, configurou retry exponencial e DLT e adicionou quatro testes com broker KRaft embarcado. O build completo aprovou 30 testes sem Docker. Também foram executadas consultas HTTP reais entre os três serviços, publicação/consumo no Kafka local, persistência PostgreSQL e reentrega após encerrar a instância anterior e iniciar outra de Candidaturas. Um helper local aguardou o offset confirmado e verificou que quantidade, pontuação e data da avaliação permaneceram iguais.

Os quatro commits separam integração HTTP/snapshots, outbox/produtor, cálculo/idempotência e listener/testes de fluxo. A evidência distingue Kafka embarcado 3.9.1 da infraestrutura declarada 4.1.1 e não atribui a esta sessão inspeção da versão binária do broker, criação de contêineres ou execução a partir de clone limpo. Cache, Pact e publicação do resultado da vaga continuam pendentes. Cabe ao responsável revisar regras de negócio e diff antes de integrar/publicar a branch.

## 2026-10-24 — Fluxo assíncrono, etapa 3

A IA implementou regras determinísticas de pontuação e avaliação única por candidatura, preservando o snapshot original. A verificação sem Docker aprovou 21 testes do módulo, incluindo reentrega e entregas concorrentes em transações reais H2. A documentação distingue essa prova de persistência da prova de consumo Kafka, que será feita na etapa seguinte. A regra de correspondência por tipo/palavra-chave/duração é explícita na ADR-005 e deve ser conferida pelo grupo.

## 2026-10-24 — Fluxo assíncrono, etapa 2

A IA implementou outbox transacional e publicação Kafka com confirmação antes da marcação. Foram aprovados 12 testes do módulo, incluindo três cenários de atomicidade e retomada com envio Kafka simulado. Não se apresentou essa simulação como prova de integração com o broker. O consumidor idempotente e a verificação com Kafka real/embarcado pertencem à etapa seguinte.

## 2026-10-24 — Fluxo assíncrono, etapa 1

O responsável solicitou concluir o fluxo em commits separados e atualizar a documentação. A IA implementou consultas HTTP com timeout, validação das referências/status e snapshots persistidos, com decisões na ADR-005. Foram aprovados nove testes do módulo de Candidaturas em compilação limpa, sem Docker. Nenhum envio Kafka foi considerado concluído nesta etapa. A revisão do diff pelo responsável permanece necessária antes de integrar a branch.

## Contribuições

Organização do roteiro e criação da estrutura inicial a partir do escopo e do enunciado fornecidos.

## Revisão humana

Pendente: validar responsabilidades, fluxo assíncrono e estrutura gerada com o grupo.

## Registro de desenvolvimento

Registrar para cada contribuição relevante: tarefa, sugestão da IA, decisão humana, correções e verificação executada. Não apresentar testes ou funcionalidades pendentes como concluídos.

### 2026-10-22 — Organização dos POMs

- **Tarefa:** revisar a relação entre o POM raiz e os POMs dos serviços após a criação das aplicações Spring Boot.
- **Sugestão da IA:** centralizar o parent do Boot e a configuração Java na raiz, fazendo os serviços herdarem de `selecao-interna`.
- **Análise crítica:** a configuração anterior era válida; o ajuste é uma escolha de manutenção, não uma condição obrigatória para compilar. Alternativas e consequências estão na [ADR-003](adr/ADR-003-heranca-maven.md).
- **Decisão humana:** o responsável pelo repositório autorizou a padronização e limitou a alteração aos POMs. As correções funcionais ficaram para o autor do código.
- **Correções de escopo:** mudanças de código e testes inicialmente adicionados pela IA foram removidos antes da validação final.
- **Verificação pela IA:** `mvn -o clean verify` na raiz, sem Docker, com sucesso em todos os módulos após a remoção das alterações funcionais. Não havia testes automatizados na versão final; build aprovado não comprova regras de negócio, integração ou compatibilidade dos contratos.
- **Revisão humana pendente:** conferir o diff final e revisar com o grupo as consequências da herança compartilhada antes do merge. Não se atribui ao grupo execução manual das verificações realizadas pela IA.

### 2026-10-22 — Persistência mínima dos serviços

- **Tarefa:** preparar a primeira etapa funcional do CORE: empregado com perfil, vaga com requisitos e candidatura registrada em bancos separados.
- **Decisão humana:** o responsável autorizou assumir a implementação nesta etapa, ampliando o escopo antes limitado aos POMs. A divisão com o autor inicial deve ser alinhada pelo responsável.
- **Sugestão e implementação pela IA:** JPA, migrações Flyway por serviço, validação de entradas e restrições únicas; preservação das entradas numéricas e adição de UUIDs para os contratos. Decisões e alternativas estão na [ADR-004](adr/ADR-004-persistencia-por-servico.md).
- **Verificação pela IA:** build completo e seis testes HTTP/JPA em H2 aprovados sem Docker; detalhes em [persistencia.md](evidencias/persistencia.md).
- **Limites e revisão pendente:** Docker não estava disponível nesta sessão. O grupo deve executar o roteiro PostgreSQL e conferir a permanência dos dados após reiniciar. A candidatura ainda não valida referências externas nem publica eventos. Não tratar unicidade de inscrição como evidência de consumidor idempotente.

### 2026-10-24 — Integração da persistência na develop

- **Decisão humana:** integrar `feat/persistencia-dados` na `develop` antes de considerar o merge para `main`.
- **Trabalho da IA:** resolver sobreposições preservando a organização em camadas de empregados, adaptar o domínio e o repositório à persistência com UUID/perfil, remover classes e dependência JPA duplicadas e manter os contratos de entrada/saída documentados.
- **Evolução funcional:** as rotas de currículo do colaborador passaram a filtrar experiências ou formação; foi acrescentado um teste de integração para esse comportamento.
- **Validação pela IA:** `mvn -o clean verify` aprovado com sete testes. Os três serviços iniciaram com PostgreSQL 17.11; cadastro, leitura, currículo filtrado, duplicata 409 e permanência dos dados após reiniciar os processos foram conferidos por HTTP. O banco já estava disponível; não foi necessário instalar Docker nem apagar volumes.
- **Limites:** o resultado não comprova Kafka, cache, Pact, consumidor idempotente ou execução a partir de clone limpo. Evidências em [persistencia.md](evidencias/persistencia.md). Revisão do diff pelo grupo e publicação no remoto continuam a cargo do responsável.


## 24/10/2026 — contrato HTTP Candidaturas → Vagas

A IA implementou Pact com cliente real do consumidor e API/JPA reais do provedor com H2, em commits separados. O plano de mensagem ResultadoPublicado foi substituído para o primeiro contrato obrigatório, justificando a escolha na ADR-007; não foi apresentada publicação como implementada. Foram usados os guias oficiais Pact JUnit 5, com versão de teste centralizada e sem Pact Broker/publicação externa.

A IA executou quatro cenários de consumidor, sete verificações do provedor e comparação automática do JSON. A primeira execução do provedor detectou estados ausentes por interação; o contrato foi corrigido e regenerado. Um experimento temporário de campo incompatível fez provedor e comparação falharem; o arquivo original foi restaurado. Build limpo completo: 54 testes aprovados, sem Docker. Uma classe inconsistente do compilador do editor foi eliminada por compilação limpa. O grupo ainda precisa revisar as expectativas, repetir a demo e conferir o escopo; a execução pela IA não comprova revisão humana. Evidência em [contrato HTTP](evidencias/contrato-http-vagas.md).


## 24–25/10/2026 — fechamento, publicação e preparação da entrega

A IA implementou barreira de admissão persistente em Candidaturas, coordenação HTTP por Vagas, outbox de resultados, transições válidas, consumo Kafka idempotente e snapshot definitivo. O trabalho foi dividido em commits por etapa. A ADR-008 registra a contenção global da barreira, fechamento monotônico/recuperação por repetição, consistência eventual e ausência de autenticação/retificação. O evento compartilhado não foi alterado e o cache parcial continua independente.

Validações pela IA: 76 testes no perfil B, falhas HTTP/timeout, transações/concorrência, Kafka/retry/DLT e execução real dos três JARs. O helper demonstrou avaliações pendentes impedindo publicação, rejeição de inscrição tardia, ranking 30/10, publicação com dispatcher pausado retomada após reiniciar Vagas, resultado preservado ao reiniciar Candidaturas e conclusão administrativa. Foram mantidos dados fictícios; só processos próprios foram encerrados, sem parar infraestrutura ou apagar volumes. A política PowerShell foi liberada somente no processo do helper.

O primeiro teste assíncrono exigiu tolerar 404 durante a espera pela projeção; o filtro JUnit foi aplicado somente aos módulos com testes, preservando shared-contracts sem dependência artificial de engine JUnit. O build limpo final passou. A execução não equivale a teste de carga, alta disponibilidade, Pact dos novos endpoints ou revisão humana. O grupo precisa conferir decisões, papéis e apresentação. Evidência: [publicação](evidencias/publicacao-resultado.md).

Validação final em 25/10: clone local limpo do commit 8276454, sem .env/target da origem, passou nos 76 testes. O helper do clone executou novamente o fluxo contra dependências locais com credenciais herdadas, até CONCLUIDA e resultado 30/10. Não houve push/merge, download de clone remoto, recriação de contêineres nem teste com Maven sem cache. [Evidência final](evidencias/entrega-final.md).
