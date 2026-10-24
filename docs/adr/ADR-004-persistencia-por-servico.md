# ADR-004 — Persistência e limites de dados por serviço

Status: aceita
Data: 2026-10-22

## Contexto

Empregados e Vagas mantinham listas em memória; Candidaturas retornava sucesso sem gravar a inscrição. O CORE exige serviços com bases segregadas. A infraestrutura já define três bancos e usuários PostgreSQL, conforme ADR-001. A ADR-002 prevê testes com H2 sem Docker.

As APIs iniciais usam matrícula e ID numéricos, enquanto os contratos de integração usam UUID. Esta etapa precisa persistir dados mínimos sem antecipar a avaliação assíncrona ou alterar os contratos compartilhados.

## Decisão

- Usar Spring Data JPA para persistência e Flyway para migrações versionadas em cada serviço. Hibernate apenas valida o schema (`ddl-auto: validate`).
- Empregados possui o cadastro, a matrícula única e os itens do perfil; Vagas possui o cadastro, o status e os requisitos. Itens e requisitos são coleções de valores pertencentes ao respectivo cadastro.
- Manter os identificadores numéricos existentes na API e acrescentar UUIDs estáveis de empregado e vaga para os contratos. O ID numérico da vaga passa a ser gerado pelo banco.
- Candidaturas possui UUID, matrícula, ID numérico da vaga e data de registro; a combinação matrícula/vaga é única no banco. Essa unicidade evita inscrições repetidas, mas não comprova idempotência de consumidor Kafka.
- Não compartilhar entidades JPA nem criar chaves estrangeiras entre bancos. As referências externas de Candidaturas são valores locais, sem consultas diretas aos outros bancos.
- Expor perfil e critérios pelos DTOs de `shared-contracts`, mantendo o prefixo real das APIs `/api/v1`.
- Executar testes HTTP/JPA na JVM com H2 em modo PostgreSQL, aplicando as migrações reais. O H2 fica restrito ao escopo de teste.

## Alternativas consideradas

| Alternativa | Benefício | Motivo para não ser a escolha |
| --- | --- | --- |
| Listas em memória | Simplicidade do protótipo | Perdem os dados ao reiniciar e não exercitam as bases segregadas |
| Schema compartilhado com chaves entre serviços | Integridade referencial centralizada | Acopla os serviços por dados e contraria o corte de domínio |
| Hibernate criando/atualizando tabelas automaticamente | Menos configuração inicial | Não mantém um histórico explícito e revisável das mudanças de schema |
| Substituir imediatamente todos os IDs numéricos por UUID | Modelo único de identificação | Quebra as entradas atuais; manter ambos permite evolução gradual sem mudar `shared-contracts` |

## Consequências e trade-offs

- Os dados são armazenados nos bancos próprios e não dependem do ciclo de vida de listas Java.
- Migrações aplicadas não devem ser editadas; evoluções exigem novos arquivos versionados.
- Restrições únicas protegem matrícula e inscrição mesmo diante de requisições concorrentes. Duplicatas retornam HTTP 409; entradas inválidas retornam 400 e consultas inexistentes retornam 404.
- Manter identificador numérico e UUID exige documentar a diferença: consulta básica de empregado usa matrícula, consulta básica de vaga usa `idVaga`, perfil usa `empregadoId` e critérios usam `vagaId`.
- A inscrição ainda não verifica existência do empregado, abertura da vaga ou critérios por HTTP. Essa integração deverá resolver as referências para UUID e capturar os snapshots antes de publicar `CandidaturaRealizada`. Portanto, esta etapa entrega o registro persistente, não o fluxo completo de inscrição/avaliação.
- A exclusão de vagas não propaga mudanças para Candidaturas. Política de exclusão e transições de status serão tratadas na evolução do fluxo.
- H2 não reproduz todas as diferenças do PostgreSQL. A validação inicial em 2026-10-22 ficou limitada ao H2; em 2026-10-24 os três serviços foram validados contra PostgreSQL 17.11, incluindo leitura após reiniciar as aplicações, conforme a evidência abaixo.

## Atualização — integração na develop (2026-10-24)

A organização em camadas de empregados foi preservada: domínio sem anotações JPA em `entity/domain`, mapeamento JPA em `entity/infra`, contrato de repositório em `repository`, implementação em `repository/impl`, Spring Data em `repository/jpa`, serviço em `service` e entrada HTTP em `interfaces`.

O adaptador foi registrado com `@Repository` e injeção por construtor; apenas ele acessa Spring Data. As coleções são convertidas para domínio dentro das transações do serviço. A entidade ganhou o construtor vazio exigido pela JPA, o mapeamento das migrações existentes e o UUID. As classes duplicadas do merge foram removidas, assim como a dependência JPA repetida no POM.

Os nomes JSON foram padronizados com a API original e a persistência: `empregadoId`, `matriculaEmpregado`, `nomeEmpregado` e `itens`. Os nomes provisórios `matricula` e `nome` da implementação intermediária não são mantidos como aliases; os consumidores devem usar o contrato documentado. Não houve mudança em `shared-contracts`.

As rotas de currículo adicionadas pelo colaborador foram mantidas e agora filtram o perfil: profissional contém experiências internas/externas; educacional contém cursos, diplomas e certificações. Ambas consultam pela matrícula e retornam 404 quando inexistente. Essa escolha foi incorporada ao teste de integração e à documentação HTTP.

## Evidências

- Entidades, repositórios, configurações e migrações em cada serviço.
- `EmpregadosPersistenceTest`, `VagasPersistenceTest` e `CandidaturasPersistenceTest`.
- [Resultado e roteiro de verificação](../evidencias/persistencia.md).
