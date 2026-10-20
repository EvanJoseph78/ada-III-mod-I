# ADR-001 — Uso de Docker no ambiente do projeto

Status: aceita
Data: 2026-10-19

## Contexto

O projeto de Seleção Interna e Movimentação de Empregados prevê microsserviços com bases segregadas, comunicação assíncrona com Kafka e cache. Esses componentes exigem um ambiente de execução reproduzível para desenvolvimento, integração e demonstração na banca.

O planejamento inicial considerava as restrições da rede corporativa da Caixa e, por isso, adotava o perfil B, baseado em JVM e sem Docker. O grupo decidiu utilizar máquina pessoal, onde Docker está disponível. A restrição que motivava o perfil B deixou de orientar o ambiente principal.

O enunciado permite execução no perfil A (Docker), B (restrito/JVM) ou C (conceitual). A rubrica também prevê testes sem Docker, requisito que deve ser considerado independentemente do ambiente principal escolhido.

## Decisão

Adotar **Docker na máquina pessoal como ambiente principal do projeto, correspondente ao perfil A**.

A infraestrutura escolhida será descrita em um `compose.yaml` versionado no repositório, com versões explícitas das imagens, configuração de rede, verificações de saúde e volumes quando houver necessidade de persistência. Esse arquivo será implementado após definir os componentes e suas versões.

Docker será usado para executar as dependências de infraestrutura, incluindo Kafka e os bancos e cache que vierem a ser escolhidos. A decisão sobre empacotar também todos os microsserviços em contêineres será detalhada na configuração de execução. Durante o desenvolvimento, eles poderão executar pela IDE ou JVM, conectando-se à infraestrutura em contêineres.

O README deverá documentar os pré-requisitos, comandos para iniciar e encerrar o ambiente, configuração necessária e procedimento da demonstração. Credenciais e segredos locais não serão versionados.

Os testes que exigirem contêineres deverão ser identificados e separados dos testes executáveis sem Docker. A estratégia concreta de testes será registrada em decisão própria.

## Alternativas consideradas

| Alternativa | Benefícios | Motivo para não ser o ambiente principal |
| --- | --- | --- |
| Perfil B: JVM com componentes embarcados | Execução em ambiente restrito e menor dependência de um runtime de contêineres | A restrição corporativa deixou de se aplicar; componentes embarcados podem exigir adaptações específicas em relação à infraestrutura escolhida |
| Instalação manual de cada dependência na máquina | Dispensa contêineres e permite configuração direta dos serviços | Aumenta o trabalho de instalação, diferenças de versões entre máquinas e dificuldade para recriar ou limpar o ambiente |
| Perfil C: solução conceitual | Permite justificar o desenho quando a execução é inviável | O grupo dispõe de ambiente para executar e demonstrar as integrações |

## Consequências e trade-offs

### Benefícios

- A configuração versionada permite recriar a infraestrutura e revisar suas alterações junto do código.
- As dependências ficam isoladas das instalações locais, reduzindo conflitos de versões e facilitando a limpeza do ambiente.
- O grupo poderá demonstrar o fluxo usando o broker e as demais dependências escolhidas para a solução.

### Custos e limitações

- A máquina de execução precisará de Docker funcional, recursos suficientes e acesso às imagens utilizadas, obtidas antes da apresentação.
- O grupo terá de configurar portas, rede, persistência e inicialização dos componentes, além de diagnosticar falhas de infraestrutura.
- Volumes exigem cuidado: recriar contêineres não deve apagar dados necessários à demonstração, e a limpeza de dados deve ser explícita.
- Docker não elimina diferenças de recursos entre máquinas nem garante, por si só, consistência, idempotência ou disponibilidade dos serviços.
- Executar a infraestrutura com Docker não atende sozinho à exigência de testes sem Docker nem comprova que o projeto já funciona no perfil A.

## Evidências e acompanhamento

- Decisão de ambiente registrada em [README.md](../../README.md#ambiente-e-execução).
- Contexto arquitetural em [arquitetura.md](../arquitetura.md).
- Perfis Maven declarados no [pom.xml](../../pom.xml), ainda como marcadores de ambiente.

Pendências para comprovar a execução desta decisão:

- [ ] Definir componentes e versões da infraestrutura.
- [ ] Criar e validar o `compose.yaml`.
- [ ] Documentar comandos e configuração no README.
- [ ] Executar o ambiente a partir de um clone limpo.
- [ ] Registrar evidência de execução no perfil A em `AVALIACAO.md`.
- [ ] Definir e verificar a estratégia de testes sem Docker.
