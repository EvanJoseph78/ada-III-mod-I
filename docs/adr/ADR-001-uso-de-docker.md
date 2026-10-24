# ADR-001 — Uso de Docker no ambiente do projeto

Status: aceita
Data: 2026-10-19
Atualizada em: 2026-10-21

## Contexto

O projeto de Seleção Interna e Movimentação de Empregados prevê microsserviços com bases segregadas, comunicação assíncrona com Kafka e cache. Esses componentes exigem um ambiente de execução reproduzível para desenvolvimento, integração e demonstração na banca.

O planejamento inicial considerava as restrições da rede corporativa da Caixa e, por isso, adotava o perfil B, baseado em JVM e sem Docker. O grupo decidiu utilizar máquina pessoal, onde Docker está disponível. A restrição que motivava o perfil B deixou de orientar o ambiente principal.

O enunciado permite execução no perfil A (Docker), B (restrito/JVM) ou C (conceitual). A rubrica também prevê testes sem Docker, requisito que deve ser considerado independentemente do ambiente principal escolhido.

## Decisão

Adotar **Docker na máquina pessoal como ambiente principal do projeto, correspondente ao perfil A**.

A infraestrutura está descrita em `compose.yaml`, versionado no repositório, com versões explícitas das imagens, verificações de saúde e volumes nomeados. Componentes adotados: Apache Kafka 4.1.1 (nó único, modo KRaft), PostgreSQL 17 e Redis 7.4 (imagem alpine). O PostgreSQL roda em um único contêiner com três bancos e três usuários, um por serviço, e cada usuário só tem acesso ao seu banco (`infra/postgres/01-create-databases.sh`).

Docker é usado para executar as dependências de infraestrutura (Kafka, PostgreSQL e Redis). Os microsserviços executam na JVM, pela IDE ou pelo Maven, e se conectam à infraestrutura em contêineres. Eles não são empacotados em imagens neste projeto.

O README deverá documentar os pré-requisitos, os comandos para iniciar e encerrar o ambiente, a configuração necessária e o procedimento da demonstração. Credenciais e segredos locais não são versionados: os valores padrão do `compose.yaml` servem apenas ao desenvolvimento local, com portas expostas somente em `127.0.0.1`, e um arquivo `.env` opcional, fora do controle de versão, permite substituí-los.

Os testes que exigirem contêineres são identificados e separados dos testes executáveis sem Docker. A estratégia de testes está registrada na [ADR-002](ADR-002-estrategia-de-testes.md).

## Alternativas consideradas

| Alternativa | Benefícios | Motivo para não ser a escolha |
| --- | --- | --- |
| Perfil B: JVM com componentes embarcados | Execução em ambiente restrito e menor dependência de um runtime de contêineres | A restrição corporativa deixou de se aplicar; componentes embarcados podem exigir adaptações específicas em relação à infraestrutura escolhida |
| Instalação manual de cada dependência na máquina | Dispensa contêineres e permite configuração direta dos serviços | Aumenta o trabalho de instalação, as diferenças de versões entre máquinas e a dificuldade para recriar ou limpar o ambiente |
| Perfil C: solução conceitual | Permite justificar o desenho quando a execução é inviável | O grupo dispõe de ambiente para executar e demonstrar as integrações |
| Um contêiner PostgreSQL por serviço | Isolamento físico de recursos e de falhas entre os bancos | Mais contêineres e mais consumo de memória na máquina pessoal, sem ganho para o escopo da demonstração |

## Consequências e trade-offs

### Benefícios

- A configuração versionada permite recriar a infraestrutura e revisar suas alterações junto do código.
- As dependências ficam isoladas das instalações locais, reduzindo conflitos de versões e facilitando a limpeza do ambiente.
- O grupo poderá demonstrar o fluxo usando o broker e as demais dependências escolhidas para a solução.
- As bases segregadas são verificáveis: cada usuário só conecta ao seu próprio banco, o que está comprovado em [execucao-infra.md](../evidencias/execucao-infra.md).

### Custos e limitações

- A máquina de execução precisará de Docker funcional, recursos suficientes e acesso às imagens utilizadas, obtidas antes da apresentação.
- O grupo terá de configurar portas, rede, persistência e inicialização dos componentes, além de diagnosticar falhas de infraestrutura.
- Volumes exigem cuidado: recriar contêineres não deve apagar dados necessários à demonstração, e a limpeza de dados deve ser explícita.
- Um único contêiner PostgreSQL oferece segregação lógica (bancos e credenciais separados), não física: os três serviços compartilham CPU, memória e o mesmo ponto de falha. A escolha reduz o custo de execução; um contêiner por serviço isolaria os recursos.
- O script de inicialização do PostgreSQL só é executado na primeira criação do volume. Alterá-lo exige recriar o volume (`docker compose down -v`), o que apaga os dados. No Windows, o script precisa ter fim de linha LF, o que é garantido por `.gitattributes`.
- Docker não elimina diferenças de recursos entre máquinas nem garante, por si só, consistência, idempotência ou disponibilidade dos serviços.
- Executar a infraestrutura com Docker não atende sozinho à exigência de testes sem Docker nem comprova que o projeto já funciona no perfil A.

## Evidências e acompanhamento

- Decisão de ambiente registrada em [README.md](../../README.md#ambiente-e-execução).
- Infraestrutura: [compose.yaml](../../compose.yaml) e [infra/postgres/01-create-databases.sh](../../infra/postgres/01-create-databases.sh).
- Execução da infraestrutura e isolamento entre bancos: [docs/evidencias/execucao-infra.md](../evidencias/execucao-infra.md).
- Contexto arquitetural em [arquitetura.md](../arquitetura.md).
- Estratégia de testes em [ADR-002](ADR-002-estrategia-de-testes.md).
- Perfis Maven declarados no [pom.xml](../../pom.xml), ainda como marcadores de ambiente.
- Estado dos critérios em [AVALIACAO.md](../../AVALIACAO.md).

Pendências para comprovar a execução desta decisão:

- [x] Definir componentes e versões da infraestrutura.
- [x] Criar e validar o `compose.yaml`.
- [ ] Documentar comandos e configuração no README.
- [ ] Executar o ambiente a partir de um clone limpo.
- [x] Registrar evidência de execução da infraestrutura em `AVALIACAO.md` (parcial: a aplicação ainda não existe).
- [x] Definir a estratégia de testes sem Docker ([ADR-002](ADR-002-estrategia-de-testes.md)).
- [x] Verificar build/testes sem Docker: 30 testes aprovados em 24/10/2026, incluindo Kafka KRaft embarcado; ver [fluxo-assincrono.md](../evidencias/fluxo-assincrono.md). Isso não encerra as pendências de clone limpo e dos demais requisitos.
